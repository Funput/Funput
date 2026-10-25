package app.funput.funput.ime.editing.backspace

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.ImeEditCommand
import app.funput.funput.ime.editing.InputConnectionEditor
import app.funput.funput.ime.editing.correction.undo.CorrectionUndo
import app.funput.funput.ime.shortcuts.undo.ShortcutUndo

/** Deletes and reopens a committed Vietnamese word as one editor transaction. */
internal class ImeBackspaceHandler(
    private val composition: AndroidCompositionSession,
    private val editor: InputConnectionEditor,
    private val connection: () -> InputConnection?,
    private val usesComposition: () -> Boolean,
    private val onCompositionChanged: () -> Unit,
    private val onCompositionCleared: () -> Unit,
) {
    private val corrections = CorrectionUndo(composition)

    fun perform() {
        val current = connection()
        // Right after a repair, Backspace takes it back and does nothing else: no delete,
        // and no reopening of the restored word either.
        if (current != null && corrections.undo(current)) return onCompositionCleared()
        if (current != null && ShortcutUndo(composition, connection).undo()) return onCompositionCleared()
        corrections.dropParked()
        if (composition.backspace(current)) return onCompositionChanged()
        if (current == null || !usesComposition()) {
            editor.execute(current, ImeEditCommand.DeleteBackward)
            onCompositionCleared()
            return
        }

        // One outer batch prevents the delete's stale selection callback from
        // closing the composition restored immediately after it.
        current.beginBatchEdit()
        try {
            editor.execute(current, ImeEditCommand.DeleteBackward)
            composition.reopenPreviousWord(current)
        } finally {
            current.endBatchEdit()
        }
        if (composition.isComposing) onCompositionChanged() else onCompositionCleared()
    }
}
