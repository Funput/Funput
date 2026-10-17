package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.composition.CommittedSelectionTracker
import app.funput.funput.ime.editing.keyevent.KeyEventBufferWriter

/**
 * Applies the engine buffer without exposing host-specific behavior to the engine session.
 *
 * Most editors get Android composing text. Broken hosts get committed replacements because they
 * ignore or decorate composing regions incorrectly. Sandbox hosts get KeyEvents only.
 */
internal class CompositionDocumentEditor(
    private val composingTextFactory: (String) -> CharSequence,
    private val keyEventWriter: KeyEventBufferWriter = KeyEventBufferWriter(),
) {
    private val committedWriter = CommittedBufferWriter()
    private val selection = CommittedSelectionTracker()

    fun update(
        connection: InputConnection,
        mode: CompositionRenderMode,
        previous: String,
        current: String,
    ): Boolean = if (mode.usesComposingSpans) {
        connection.setComposingText(composingTextFactory(current), CursorAfterText)
    } else {
        replaceBeforeCursor(connection, mode, previous, current)
    }

    fun finish(connection: InputConnection?, mode: CompositionRenderMode, active: Boolean) {
        if (active && mode.usesComposingSpans) connection?.finishComposingText()
    }

    fun commitBoundary(
        connection: InputConnection,
        mode: CompositionRenderMode,
        previous: String,
        replacement: String?,
        boundary: String,
    ): Boolean = if (mode.usesComposingSpans) {
        if (replacement != null) connection.commitText(replacement, CursorAfterText)
        else {
            connection.finishComposingText()
            connection.commitText(boundary, CursorAfterText)
        }
    } else if (replacement != null) {
        replaceBeforeCursor(connection, mode, previous, replacement)
    } else {
        replaceBeforeCursor(connection, mode, "", boundary)
    }

    fun acceptSuggestion(
        connection: InputConnection,
        mode: CompositionRenderMode,
        prefix: String,
        candidate: String,
    ): Boolean = if (mode.usesComposingSpans) {
        connection.commitText("$candidate ", CursorAfterText)
    } else {
        replaceBeforeCursor(connection, mode, prefix, "$candidate ")
    }

    fun reopenWord(
        connection: InputConnection,
        mode: CompositionRenderMode,
        word: String,
    ): Boolean = if (mode.usesComposingSpans) {
        if (!connection.deleteSurroundingText(word.length, 0)) return false
        if (connection.setComposingText(composingTextFactory(word), CursorAfterText)) return true
        connection.commitText(word, CursorAfterText)
        false
    } else true.also { selection.forget() }

    /** Replaces [previous], already committed right before the caret, with [replacement]. */
    fun replaceCommitted(
        connection: InputConnection,
        mode: CompositionRenderMode,
        previous: String,
        replacement: String,
    ): Boolean = if (mode.usesComposingSpans) {
        connection.beginBatchEdit()
        try {
            connection.deleteSurroundingText(previous.length, 0) &&
                connection.commitText(replacement, CursorAfterText)
        } finally {
            connection.endBatchEdit()
        }
    } else {
        replaceBeforeCursor(connection, mode, previous, replacement)
    }

    fun ownsSelection(
        connection: InputConnection?,
        mode: CompositionRenderMode,
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        composingEnd: Int,
    ): Boolean = if (mode.usesComposingSpans) {
        selectionStart == composingEnd && selectionEnd == composingEnd
    } else {
        selection.owns(connection, text, selectionStart, selectionEnd)
    }

    private fun replaceBeforeCursor(
        connection: InputConnection,
        mode: CompositionRenderMode,
        previous: String,
        replacement: String,
    ): Boolean {
        selection.expect(previous, replacement)
        return if (mode.writesKeyEvents) {
            keyEventWriter.replace(connection, previous, replacement)
        } else {
            committedWriter.replace(connection, previous, replacement, mode.deleteWithKeyEvents)
        }
    }

    private companion object {
        const val CursorAfterText = 1
    }
}
