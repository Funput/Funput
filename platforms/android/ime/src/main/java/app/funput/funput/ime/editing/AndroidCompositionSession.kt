package app.funput.funput.ime.editing

import android.text.SpannableString
import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.composition.CompositionBoundary
import app.funput.funput.ime.editing.composition.CursorAfterText
import app.funput.funput.ime.editing.composition.singleCodePointOrNull
import app.funput.funput.ime.editing.correction.TypoCorrection
import app.funput.funput.ime.editing.reopen.PreviousWordReopener
import app.funput.funput.ime.nativebridge.VietnameseEngine
import app.funput.funput.keyboard.touch.KeyTouchEvidence

/** Maps the shared engine buffer onto Android's composing-text primitives. */
internal class AndroidCompositionSession(
    internal val engine: VietnameseEngine,
    private val composingTextFactory: (String) -> CharSequence = ::unstyledComposingText,
    internal val corrections: TypoCorrection = TypoCorrection(engine.correction),
) {
    private val documentEditor = CompositionDocumentEditor(composingTextFactory)
    private val previousWord = PreviousWordReopener(engine::adopt)
    private var renderMode = CompositionRenderMode.COMPOSING
    var composingText: String = ""
        private set
    /** The word a boundary just finished, and whether that boundary was a space. */
    private var completed: Pair<String, Boolean>? = null

    val isComposing: Boolean get() = composingText.isNotEmpty()

    fun setEnabled(enabled: Boolean) = engine.setEnabled(enabled)

    fun setRenderMode(value: CompositionRenderMode) { renderMode = value }

    fun isBoundary(codePoint: Int): Boolean =
        CompositionBoundary.isBoundary(codePoint, engine.inputMethod)

    /** [touch] is where the finger landed for [text], when it was a tap on a letter. */
    fun input(connection: InputConnection?, text: String, touch: KeyTouchEvidence? = null): Boolean {
        completed = null
        if (connection == null || text.isEmpty()) return false
        val codePoint = text.singleCodePointOrNull() ?: return commitRaw(connection, text)
        corrections.keyTyped(touch)
        return if (CompositionBoundary.isBoundary(codePoint, engine.inputMethod)) {
            commitBoundary(connection, text, codePoint)
        } else {
            updateComposing(connection, engine.process(codePoint), text)
        }
    }

    fun backspace(connection: InputConnection?): Boolean {
        completed = null
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
        completed = null
        previousWord.clear()
        corrections.reset()
        engine.clear()
    }

    fun takeCompleted(): Pair<String, Boolean>? = completed.also { completed = null }

    fun acceptSuggestion(connection: InputConnection, prefix: String, candidate: String): Boolean {
        if (composingText != prefix) return false
        val accepted = documentEditor.acceptSuggestion(connection, renderMode, prefix, candidate)
        if (accepted) previousWord.recorded(prefix, "$candidate ")
        composingText = ""
        completed = null
        engine.clear()
        return accepted
    }

    private fun commitBoundary(
        connection: InputConnection,
        text: String,
        codePoint: Int,
    ): Boolean {
        val previous = composingText
        val replacement = corrections.atBoundary(previous, engine.processBoundary(codePoint), text)
        // Only a space leaves the words either side of it adjacent; the tracker
        // has no other way to know which boundary this was.
        val token = replacement?.removeSuffix(text)?.ifEmpty { null } ?: previous.ifEmpty { null }
        completed = token?.let { it to (codePoint == ' '.code) }
        previousWord.recorded(previous, replacement ?: previous + text)
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

    /** Re-opens the word behind the caret, right after Backspace, so the next keystroke can retone it. */
    fun reopenPreviousWord(connection: InputConnection?): Boolean {
        if (connection == null || isComposing) return false
        if (!connection.getSelectedText(0).isNullOrEmpty()) return false
        val word = previousWord.wordToReopen(connection) ?: return false

        connection.beginBatchEdit()
        val reopened = try {
            documentEditor.reopenWord(connection, renderMode, word)
        } finally {
            connection.endBatchEdit()
        }
        if (reopened) composingText = word else engine.clear()
        return reopened
    }

    /** Whether the live composition has lost the caret reported by the editor and must finish. */
    fun disownsSelection(connection: InputConnection?, start: Int, end: Int, composingEnd: Int): Boolean {
        val owned = isComposing &&
            documentEditor.ownsSelection(connection, renderMode, composingText, start, end, composingEnd)
        previousWord.caretReported(start, end, owned)
        return isComposing && !owned
    }

    /** Rewrites [previous], committed right before the caret, as [replacement] in one batch. */
    fun replaceCommitted(connection: InputConnection, previous: String, replacement: String): Boolean {
        if (!documentEditor.replaceCommitted(connection, renderMode, previous, replacement)) return false
        previousWord.rewrote(previous, replacement)
        return true
    }

    private fun commitRaw(connection: InputConnection, text: String): Boolean {
        finish(connection)
        return connection.commitText(text, CursorAfterText)
    }
}

private fun unstyledComposingText(text: String): CharSequence = SpannableString(text)
