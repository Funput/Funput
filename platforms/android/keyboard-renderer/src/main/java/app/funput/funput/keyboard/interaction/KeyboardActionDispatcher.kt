package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.model.toKeyAction

/** Applies keyboard modifier state before forwarding semantic actions to the host. */
internal class KeyboardActionDispatcher(
    private val keySpec: (keyId: String) -> KeySpec?,
    private val onAction: (KeyAction) -> Unit,
    private val onShiftStateChanged: () -> Unit,
    doubleTapTimeoutMillis: Long,
) {
    private val shiftController = ShiftStateController(doubleTapTimeoutMillis)

    val shiftState: ShiftState get() = shiftController.state

    fun dispatch(keyId: String, eventTimeMillis: Long) {
        val key = keySpec(keyId) ?: return
        if (key.role == KeyRole.SHIFT) {
            shiftController.onShiftReleased(eventTimeMillis)
            onShiftStateChanged()
        }

        val action = key.toKeyAction(shiftState) ?: return
        onAction(action)
        if (shiftController.consumeAfter(key.role)) onShiftStateChanged()
    }

    fun repeatBackspace() = onAction(KeyAction.Backspace)

    fun toggleLanguage(language: KeyboardLanguage) = onAction(KeyAction.ToggleLanguage(language))

    fun setShiftState(value: ShiftState) {
        if (shiftController.setState(value)) onShiftStateChanged()
    }

    fun reset() {
        if (shiftController.reset()) onShiftStateChanged()
    }
}
