package app.funput.funput.ime.editing.backspace

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.correction.undo.CorrectionUndo
import app.funput.funput.ime.editing.recentText
import app.funput.funput.ime.shortcuts.undo.ShortcutUndo

/** Deletes and reopens a committed Vietnamese word as one editor transaction. */
internal class ImeBackspaceHandler(
    private val composition: AndroidCompositionSession,
    private val editor: InputConnectionEditor,
    private val connection: () -> InputConnection?,
    private val usesComposition: () -> Boolean,
    private val onCompositionChanged: () -> Unit,
    private val onCompositionCleared: () -> Unit,
    private val rawKeyEditor: () -> Boolean = { false },
) {
    private val corrections = CorrectionUndo(composition)

    /** A raw-key editor keeps no text for an edit to delete; only the key press reaches it. */
    private val plainDelete: ImeEditCommand
        get() = if (rawKeyEditor()) ImeEditCommand.PressDeleteKey else ImeEditCommand.DeleteBackward

    /**
     * Deletes the user's selection, if there is one, as a plain range edit and returns true.
     * The range is unrelated to the words just typed, so undoing a repair or reopening a word
     * would edit text outside it. A live composition owns the caret, so mid-word Backspace never
     * pays for asking the editor.
     */
    fun deleteSelection(): Boolean {
        val current = connection() ?: return false
        if (composition.isComposing || current.getSelectedText(0).isNullOrEmpty()) return false
        corrections.dropParked()
        editor.execute(current, ImeEditCommand.DeleteBackward)
        return true
    }

    /** Backspace with no selection; [deleteSelection] has already been offered the keystroke. */
    fun perform() {
        val current = connection()
        // Right after a repair, Backspace takes it back and does nothing else: no delete,
        // and no reopening of the restored word either.
        if (current != null && corrections.undo(current)) return onCompositionCleared()
        if (current != null && ShortcutUndo(composition, connection).undo()) return onCompositionCleared()
        corrections.dropParked()
        if (composition.backspace(current)) return onCompositionChanged()
        // A raw-key editor never shows a composition, so reopening a word there would take it
        // off the screen until the next commit.
        if (current == null || !usesComposition() || rawKeyEditor()) {
            editor.execute(current, plainDelete)
            onCompositionCleared()
            return
        }

        // One outer batch prevents the delete's stale selection callback from
        // closing the composition restored immediately after it.
        current.beginBatchEdit()
        try {
            // Read first: some hosts still answer with this text after the delete.
            val before = current.recentText()
            editor.execute(current, ImeEditCommand.DeleteBackward)
            composition.reopenPreviousWord(current, before)
        } finally {
            current.endBatchEdit()
        }
        if (composition.isComposing) onCompositionChanged() else onCompositionCleared()
    }
}
