package app.funput.funput.ime.editing.capitalization

import app.funput.funput.ime.editing.EditorInfoPolicy
import app.funput.funput.keyboard.model.ShiftState

/**
 * Synchronizes one-shot Shift with where the caret is.
 *
 * Recomputed from the document rather than accumulated: a caret moves for reasons
 * no keystroke explains — a paste, a tap elsewhere, a suggestion taken, a field
 * that opened with text already in it — so any running state would drift.
 */
internal class AutoCapitalizationController(
    private val boundary: CapitalizationBoundary,
    private val textBeforeCursor: () -> CharSequence?,
    private val currentShiftState: () -> ShiftState,
    private val updateShiftState: (ShiftState) -> Unit,
) {
    private var policy: EditorInfoPolicy? = null
    private var enabled = true
    private var mode = AutoCapitalizationMode.NONE

    /** `null` means no editor is attached, which capitalizes nothing. */
    fun configure(policy: EditorInfoPolicy?) {
        this.policy = policy
        mode = policy?.autoCapitalizationMode(enabled) ?: AutoCapitalizationMode.NONE
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
        configure(policy)
    }

    fun update(preserveCapsLock: Boolean = true) {
        if (preserveCapsLock && currentShiftState() == ShiftState.CAPS_LOCK) return
        val uppercase = boundary.uppercases(mode, textBeforeCursor() ?: "")
        updateShiftState(if (uppercase) ShiftState.ON else ShiftState.OFF)
    }
}
