package app.funput.funput.ime.editing

import android.text.TextUtils
import app.funput.funput.keyboard.model.ShiftState

/** Synchronizes one-shot Shift with the editor's capitalization state. */
internal class AutoCapitalizationController(
    private val cursorCapsMode: (requestedModes: Int) -> Int,
    private val currentShiftState: () -> ShiftState,
    private val updateShiftState: (ShiftState) -> Unit,
) {
    private var requestedModes = 0
    private var enabled = true

    fun configure(capitalizationModes: Int) {
        requestedModes = capitalizationModes
    }

    fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    fun update(preserveCapsLock: Boolean = true) {
        if (preserveCapsLock && currentShiftState() == ShiftState.CAPS_LOCK) return
        val modes = if (enabled) requestedModes else requestedModes and TextUtils.CAP_MODE_CHARACTERS
        val shouldCapitalize = modes != 0 && cursorCapsMode(modes) != 0
        updateShiftState(if (shouldCapitalize) ShiftState.ON else ShiftState.OFF)
    }
}
