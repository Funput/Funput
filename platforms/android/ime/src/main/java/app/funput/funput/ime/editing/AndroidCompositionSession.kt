package app.funput.funput.ime.editing

import android.text.SpannableString
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.composition.CompositionBoundary
import app.funput.funput.ime.editing.composition.CursorAfterText
import app.funput.funput.ime.editing.composition.singleCodePointOrNull
import app.funput.funput.ime.nativebridge.VietnameseEngine

/** Maps the shared engine buffer onto Android's composing-text primitives. */
internal class AndroidCompositionSession(
    private val engine: VietnameseEngine,
    private val composingTextFactory: (String) -> CharSequence = ::unstyledComposingText,
) {
    private val documentEditor = CompositionDocumentEditor(composingTextFactory)
    private val committedTail = CommittedWordTail()
    private var renderMode = CompositionRenderMode.COMPOSING
    var composingText: String = ""
        private set
    private var completedToken: String? = null

    val isComposing: Boolean get() = composingText.isNotEmpty()

    fun setEnabled(enabled: Boolean) = engine.setEnabled(enabled)

    fun setRenderMode(value: CompositionRenderMode) {
        renderMode = value
    }

    fun input(connection: InputConnection?, text: String): Boolean {
        completedToken = null
        if (connection == null || text.isEmpty()) return false
        val codePoint = text.singleCodePointOrNull() ?: return commitRaw(connection, text)
        return if (CompositionBoundary.isBoundary(codePoint, engine.inputMethod)) {
            commitBoundary(connection, text, codePoint)
        } else {
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
        committedTail.clear()
        engine.clear()
    }

    fun takeCompletedToken(): String? = completedToken.also { completedToken = null }

    fun acceptSuggestion(connection: InputConnection, prefix: String, candidate: String): Boolean {
        if (composingText != prefix) return false
        val accepted = documentEditor.acceptSuggestion(connection, renderMode, prefix, candidate)
        if (accepted) committedTail.record("$candidate ")
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
        committedTail.record(replacement ?: previous + text)
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
     * Re-opens the word behind the caret so the next keystroke can retone it.
     * Walks [committedTail] when the editor withholds or stale-reads surrounding text.
     */
    fun reopenPreviousWord(connection: InputConnection?): Boolean {
        if (connection == null || isComposing) return false
        if (!connection.getSelectedText(0).isNullOrEmpty()) return false
        val word = listOfNotNull(committedTail.backspace(), connection.wordBeforeCursor())
            .distinct()
            .firstOrNull(engine::adopt)
        committedTail.resolve(word != null)
        if (word == null) return false

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

}

private fun unstyledComposingText(text: String): CharSequence = SpannableString(text)
