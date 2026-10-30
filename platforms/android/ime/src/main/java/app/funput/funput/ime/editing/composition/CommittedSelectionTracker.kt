package app.funput.funput.ime.editing.composition

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.deletesBeforeCommit

/**
 * Tells our own caret moves from the user's in hosts without composing spans, where the only
 * evidence of a write is the selection update the editor sends back afterwards.
 */
internal class CommittedSelectionTracker {
    private val caret = ExpectedCaret()
    private var midEditSelection: Int? = null

    /** Records that [previous] before the caret is about to become [replacement]. */
    fun expect(previous: String, replacement: String) {
        val afterDelete = caret.expect(previous.length, replacement.length)
        midEditSelection = afterDelete?.takeIf { deletesBeforeCommit(previous, replacement) }
    }

    /** Whether a caret at [selectionStart]..[selectionEnd] still sits right after [text]. */
    fun owns(connection: InputConnection?, text: String, selectionStart: Int, selectionEnd: Int): Boolean {
        if (selectionStart != selectionEnd) return false
        // Gecko textareas carrying `maxlength` report the caret from inside our batch
        // edit, after the delete and before the commit. That caret is ours, not a move.
        if (midEditSelection == selectionEnd) {
            midEditSelection = null
            return true
        }
        val beforeCursor = connection?.getTextBeforeCursor(text.length, 0)?.toString()
        val editorConfirmsText = beforeCursor?.endsWith(text) == true
        val editorWithholdsText = beforeCursor.isNullOrEmpty() && selectionEnd >= text.length
        val positionMatches = caret.target?.let { it == selectionEnd } ?: true
        val owned = editorConfirmsText || (editorWithholdsText && positionMatches)
        if (owned) {
            caret.confirm(selectionEnd)
            midEditSelection = null
        }
        return owned
    }

    /** Forgets every position: the text it described is no longer ours. */
    fun forget() {
        caret.forget()
        midEditSelection = null
    }
}
