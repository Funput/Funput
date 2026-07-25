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
    var composingText: String = ""
        private set
    private var completedToken: String? = null

    val isComposing: Boolean get() = composingText.isNotEmpty()

    fun setEnabled(enabled: Boolean) = engine.setEnabled(enabled)

    fun input(connection: InputConnection?, text: String): Boolean {
        completedToken = null
        if (connection == null || text.isEmpty()) return false
        val codePoint = text.singleCodePointOrNull() ?: return commitRaw(connection, text)
        return if (CompositionBoundary.isBoundary(codePoint)) {
            commitBoundary(connection, text, codePoint)
        } else {
            updateComposing(connection, engine.process(codePoint), text)
        }
    }

    fun backspace(connection: InputConnection?): Boolean {
        completedToken = null
        if (connection == null || !isComposing) return false
        composingText = engine.backspace()
        return connection.setComposingText(composingTextFactory(composingText), CursorAfterText)
    }

    fun finish(connection: InputConnection?) {
        if (isComposing) connection?.finishComposingText()
        reset()
    }

    fun reset() {
        composingText = ""
        completedToken = null
        engine.clear()
    }

    fun takeCompletedToken(): String? = completedToken.also { completedToken = null }

    fun acceptSuggestion(connection: InputConnection, prefix: String, candidate: String): Boolean {
        if (composingText != prefix) return false
        composingText = ""
        completedToken = null
        engine.clear()
        return connection.commitText("$candidate ", CursorAfterText)
    }

    private fun commitBoundary(
        connection: InputConnection,
        text: String,
        codePoint: Int,
    ): Boolean {
        val previous = composingText
        val replacement = engine.processBoundary(codePoint)
        completedToken = replacement?.removeSuffix(text)?.ifEmpty { null } ?: previous.ifEmpty { null }
        composingText = ""
        if (replacement != null) return connection.commitText(replacement, CursorAfterText)
        connection.finishComposingText()
        return connection.commitText(text, CursorAfterText)
    }

    private fun updateComposing(
        connection: InputConnection,
        buffer: String,
        rawText: String,
    ): Boolean {
        composingText = buffer
        return if (buffer.isEmpty()) commitRaw(connection, rawText) else {
            connection.setComposingText(composingTextFactory(buffer), CursorAfterText)
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
        val word = connection.wordBeforeCursor() ?: return false
        if (!engine.adopt(word)) return false

        connection.beginBatchEdit()
        val reopened = try {
            connection.deleteSurroundingText(word.length, 0) &&
                connection.setComposingText(composingTextFactory(word), CursorAfterText)
        } finally {
            connection.endBatchEdit()
        }
        if (reopened) composingText = word else engine.clear()
        return reopened
    }

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
