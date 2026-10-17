package app.funput.funput.ime.editing.correction.undo

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession

/**
 * Backspace right after a repair puts back what was typed, boundary and all, and remembers
 * the word so it is not corrected again. Any other key spends the chance.
 */
internal class CorrectionUndo(private val composition: AndroidCompositionSession) {
    private val state get() = composition.corrections

    /** True when Backspace took a repair back and must do nothing else. */
    fun undo(connection: InputConnection): Boolean {
        val applied = state.applied ?: return false
        state.applied = null
        if (!state.engine.hasUndo()) return false
        // Check the document before asking the engine: asking spends the undo, and a caret
        // that moved since means the repair is no longer what Backspace would delete.
        val before = connection.getTextBeforeCursor(applied.corrected.length, 0)?.toString()
        if (before != applied.corrected) return false
        val edit = state.engine.undo() ?: return false
        if (!composition.replaceCommitted(connection, applied.corrected, edit.text)) return false
        state.dictionary?.let { dictionary ->
            dictionary.keep(applied.typed)
            if (applied.composed.isNotEmpty() && applied.composed != applied.typed) dictionary.keep(applied.composed)
        }
        return true
    }

    /** A repair left on the bar after Space lapses with Backspace; tell the engine so. */
    fun dropParked() {
        if (state.parked == null) return
        state.parked = null
        state.engine.apply(null)
    }
}
