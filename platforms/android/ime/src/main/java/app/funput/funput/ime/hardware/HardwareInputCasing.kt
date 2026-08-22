package app.funput.funput.ime.hardware

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.ShiftState

/**
 * Applies the hidden soft-keyboard shift state to a hardware [KeyAction.Input].
 *
 * Physical Shift/Caps already sit in `unicodeChar`; this covers Funput auto-cap.
 */
internal fun KeyAction.applyHardwareCasing(shift: ShiftState): KeyAction {
    val input = this as? KeyAction.Input ?: return this
    if (!shift.isActive) return this
    val first = input.text.firstOrNull() ?: return this
    if (!first.isLowerCase()) return this
    return input.copy(text = input.text.replaceFirstChar { it.titlecaseChar() })
}
