package app.funput.funput.ime.editing

import android.text.SpannableString
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.model.KeyboardInputMethod

/** Maps the shared engine buffer onto Android's composing-text primitives. */
internal class AndroidCompositionSession(
    private val engine: VietnameseEngine,
    private val composingTextFactory: (String) -> CharSequence = ::unstyledComposingText,
) {
    private val documentEditor = CompositionDocumentEditor(composingTextFactory)
    private var renderMode = CompositionRenderMode.COMPOSING
    var composingText: String = ""
        private set
    private var completedToken: String? = null
    private var recentCompletedToken: String? = null

    val isComposing: Boolean get() = composingText.isNotEmpty()

    fun setEnabled(enabled: Boolean) = engine.setEnabled(enabled)

    fun setRenderMode(value: CompositionRenderMode) {
        renderMode = value
    }

    fun input(connection: InputConnection?, text: String): Boolean {
        completedToken = null
        if (connection == null || text.isEmpty()) return false
        val codePoint = text.singleCodePointOrNull() ?: return commitRaw(connection, text)
        return if (CompositionBoundary.isBoundary(codePoint)) {
            commitBoundary(connection, text, codePoint)
        } else {
            recentCompletedToken = null
            updateComposing(connection, engine.process(codePoint), text)
        }
    }

    fun backspace(connection: InputConnection?): Boolean {
        completedToken = null
        if (connection == null || !isComposing) return false
        val previous = composingText
        composingText = engine.backspace()
        return documentEditor.update(connection, renderMode, previous, composingText)
    }

    fun finish(connection: InputConnection?) {
        documentEditor.finish(connection, renderMode, isComposing)
        reset()
    }

    fun reset() {
        composingText = ""
        completedToken = null
        recentCompletedToken = null
        engine.clear()
    }

    fun takeCompletedToken(): String? = completedToken.also { completedToken = null }

    fun acceptSuggestion(connection: InputConnection, prefix: String, candidate: String): Boolean {
        if (composingText != prefix) return false
        val accepted = documentEditor.acceptSuggestion(connection, renderMode, prefix, candidate)
        composingText = ""
        completedToken = null
        engine.clear()
        return accepted
    }

    private fun commitBoundary(
        connection: InputConnection,
        text: String,
        codePoint: Int,
    ): Boolean {
        val previous = composingText
        val replacement = engine.processBoundary(codePoint)
        completedToken = replacement?.removeSuffix(text)?.ifEmpty { null } ?: previous.ifEmpty { null }
        recentCompletedToken = completedToken
        composingText = ""
        return documentEditor.commitBoundary(connection, renderMode, previous, replacement, text)
    }

    private fun updateComposing(
        connection: InputConnection,
        buffer: String,
        rawText: String,
    ): Boolean {
        val previous = composingText
        composingText = buffer
        return if (buffer.isEmpty()) commitRaw(connection, rawText) else {
            documentEditor.update(connection, renderMode, previous, buffer)
        }
    }

    /**
     * Re-opens the word the caret now sits behind, so the next keystroke edits it
     * instead of typing a literal letter (`chào` ⌫ then `s` gives `cháo`).
     *
     * Called only from the Backspace path that did *not* have a live composition, so
     * typing never pays for it. Uses one bounded [wordBeforeCursor] read, on a
     * keystroke where the editor is already being consulted, and
     * asks the engine before touching the document, so a non-Vietnamese word is left
     * exactly as it was.
     */
    fun reopenPreviousWord(connection: InputConnection?): Boolean {
        if (connection == null || isComposing) return false
        if (!connection.getSelectedText(0).isNullOrEmpty()) return false
        val word = listOfNotNull(connection.wordBeforeCursor(), recentCompletedToken)
            .distinct()
            .firstOrNull(engine::adopt)
            ?: return false
        recentCompletedToken = null

        connection.beginBatchEdit()
        val reopened = try {
            documentEditor.reopenWord(connection, renderMode, word)
        } finally {
            connection.endBatchEdit()
        }
        if (reopened) composingText = word else engine.clear()
        return reopened
    }

    fun ownsSelection(
        connection: InputConnection?,
        selectionStart: Int,
        selectionEnd: Int,
        composingEnd: Int,
    ): Boolean = documentEditor.ownsSelection(
        connection,
        renderMode,
        composingText,
        selectionStart,
        selectionEnd,
        composingEnd,
    )

    private fun commitRaw(connection: InputConnection, text: String): Boolean {
        finish(connection)
        return connection.commitText(text, CursorAfterText)
    }

    private fun String.singleCodePointOrNull(): Int? {
        val first = codePointAt(0)
        return first.takeIf { Character.charCount(it) == length }
    }

    private companion object {
        const val CursorAfterText = 1
    }
}

private fun unstyledComposingText(text: String): CharSequence = SpannableString(text)

/** Mirrors the shared engine's current word-boundary contract. */
internal object CompositionBoundary {
    fun isBoundary(codePoint: Int): Boolean =
        Character.isWhitespace(codePoint) || codePoint.isAsciiPunctuation()

    private fun Int.isAsciiPunctuation(): Boolean =
        this in 0x21..0x2F ||
            this in 0x3A..0x40 ||
            this in 0x5B..0x60 ||
            this in 0x7B..0x7E
}
