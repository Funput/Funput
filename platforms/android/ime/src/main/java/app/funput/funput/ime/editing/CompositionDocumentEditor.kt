package app.funput.funput.ime.editing

import android.view.inputmethod.InputConnection
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
    private var committedSelection: Int? = null
    private var expectedCommittedSelection: Int? = null
    private var midEditCommittedSelection: Int? = null

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
        connection.deleteSurroundingText(word.length, 0) &&
            connection.setComposingText(composingTextFactory(word), CursorAfterText)
    } else true.also {
        committedSelection = null
        expectedCommittedSelection = null
        midEditCommittedSelection = null
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
        ownsCommittedSelection(connection, text, selectionStart, selectionEnd)
    }

    private fun replaceBeforeCursor(
        connection: InputConnection,
        mode: CompositionRenderMode,
        previous: String,
        replacement: String,
    ): Boolean {
        expectCommittedSelection(previous, replacement)
        return if (mode.writesKeyEvents) {
            keyEventWriter.replace(connection, previous, replacement)
        } else {
            committedWriter.replace(connection, previous, replacement, mode.deleteWithKeyEvents)
        }
    }

    private fun ownsCommittedSelection(
        connection: InputConnection?,
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
    ): Boolean {
        if (selectionStart != selectionEnd) return false
        // Gecko textareas carrying `maxlength` report the caret from inside our batch
        // edit, after the delete and before the commit. That caret is ours, not a move.
        if (midEditCommittedSelection == selectionEnd) {
            midEditCommittedSelection = null
            return true
        }
        val beforeCursor = connection?.getTextBeforeCursor(text.length, 0)?.toString()
        val editorConfirmsText = beforeCursor?.endsWith(text) == true
        val editorWithholdsText = beforeCursor.isNullOrEmpty() && selectionEnd >= text.length
        val positionMatches = expectedCommittedSelection?.let { it == selectionEnd } ?: true
        val owned = editorConfirmsText || (editorWithholdsText && positionMatches)
        if (owned) {
            committedSelection = selectionEnd
            expectedCommittedSelection = null
            midEditCommittedSelection = null
        }
        return owned
    }

    private fun expectCommittedSelection(previous: String, replacement: String) {
        val base = expectedCommittedSelection ?: committedSelection
        val afterDelete = base?.minus(previous.length)
        midEditCommittedSelection = afterDelete
            ?.takeIf { deletesBeforeCommit(previous, replacement) }
        expectedCommittedSelection = afterDelete?.plus(replacement.length)
    }

    private companion object {
        const val CursorAfterText = 1
    }
}
