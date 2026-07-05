package app.funput.funput.ime.editing

import app.funput.funput.keyboard.model.ShiftState

/** Synchronizes one-shot Shift with the editor's capitalization state. */
internal class AutoCapitalizationController(
    private val cursorCapsMode: (requestedModes: Int) -> Int,
    private val currentShiftState: () -> ShiftState,
    private val updateShiftState: (ShiftState) -> Unit,
) {
    private var requestedModes = 0

    fun configure(capitalizationModes: Int) {
        requestedModes = capitalizationModes
    }

    fun update(preserveCapsLock: Boolean = true) {
        if (preserveCapsLock && currentShiftState() == ShiftState.CAPS_LOCK) return
        val shouldCapitalize = requestedModes != 0 && cursorCapsMode(requestedModes) != 0
        updateShiftState(if (shouldCapitalize) ShiftState.ON else ShiftState.OFF)
    }
}
