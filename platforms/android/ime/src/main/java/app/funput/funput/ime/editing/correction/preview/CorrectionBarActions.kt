package app.funput.funput.ime.editing.correction.preview

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.AndroidCompositionSession
import app.funput.funput.ime.editing.correction.AppliedCorrection
import app.funput.funput.ime.editing.correction.dropLastScalars
import app.funput.funput.ime.editing.correction.scalarCount
import app.funput.funput.ime.editing.correction.undo.CorrectionUndo

/**
 * What a tap on a correction chip does. Mirrors iOS `KeyboardInputCoordinator+PreviewActions`:
 * the typed word keeps it, a candidate writes it in, the undo puts the typed word back.
 */
internal class CorrectionBarActions(
    private val composition: AndroidCompositionSession,
    private val connection: () -> InputConnection?,
    /** Types a Space through the ordinary key path, so the boundary applies the repair. */
    private val space: () -> Unit,
) {
    private val state get() = composition.corrections
    private val undo = CorrectionUndo(composition)

    /** The user wants [typed] exactly as it is. */
    fun refuse(typed: String): Boolean {
        val parked = state.parked
        if (parked == null && composition.composingText != typed) return false
        state.engine.refuse()
        state.parked = null
        state.dictionary?.keep(parked?.shown ?: typed)
        state.declines.refused += 1
        return true
    }

    /** The user picked [text]: before Space, Space applies it; after, it replaces the word. */
    fun accept(text: String): Boolean {
        if (state.parked != null) return replaceParked(text)
        if (!composition.isComposing) return false
        state.forced = text
        try {
            space()
        } finally {
            state.forced = null
        }
        return true
    }

    fun undo(): Boolean = connection()?.let(undo::undo) == true

    private fun replaceParked(text: String): Boolean {
        val parked = state.parked ?: return false
        state.parked = null
        val engine = state.engine
        // Read before answering: applying spends what the engine has parked.
        val backspace = engine.pendingBackspace()
        val index = engine.candidates().indexOf(text)
        val current = connection()
        val before = current?.getTextBeforeCursor(parked.written.length, 0)?.toString()
        if (current == null || index < 0 || before != parked.written || backspace > parked.written.scalarCount()) {
            engine.apply(null)
            return false
        }
        val edit = engine.apply(index) ?: return false
        val replacement = parked.written.dropLastScalars(edit.backspace) + edit.text
        if (!composition.replaceCommitted(current, parked.written, replacement)) return false
        state.applied = AppliedCorrection(typed = parked.shown, composed = parked.shown, corrected = edit.text)
        return true
    }
}
