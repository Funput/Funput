package app.funput.funput.ime.hardware

import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.ImeKeyboardCallbackBinder
import app.funput.funput.ime.hardware.visibility.SoftKeyboardVisibility
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.ShiftState

/**
 * Consumes hardware key downs that Funput can type, and the matching ups.
 *
 * Repeats are dispatched (held Backspace). Ups never type a second character.
 */
internal class ImeHardwareKeyHandler(
    private val currentShift: () -> ShiftState,
    private val dispatch: (KeyAction) -> Unit,
    private val finish: () -> Unit,
    private val hotkeyEnabled: () -> Boolean = { false },
    private val toggleSoftKeyboard: () -> Unit = {},
) {
    private val consumedDown = mutableSetOf<Int>()

    fun onKeyDown(stroke: HardwareKeyStroke): Boolean =
        when (val decision = HardwareKeyActionMapper.map(stroke, hotkeyEnabled())) {
            is HardwareKeyDecision.Consume -> {
                dispatch(decision.action.applyHardwareCasing(currentShift()))
                consumedDown.add(stroke.keyCode)
                true
            }
            HardwareKeyDecision.ToggleSoftKeyboard -> {
                // A held hotkey repeats; only the first down flips the keyboard. Like any other
                // modifier chord it ends the word, so the keyboard appears over settled text.
                if (stroke.repeatCount == 0) {
                    finish()
                    toggleSoftKeyboard()
                }
                consumedDown.add(stroke.keyCode)
                true
            }
            HardwareKeyDecision.FinishAndPass -> {
                finish()
                false
            }
            HardwareKeyDecision.Pass -> false
        }

    fun onKeyUp(stroke: HardwareKeyStroke): Boolean = consumedDown.remove(stroke.keyCode)

    companion object {
        fun bind(
            session: ImeEditingSession,
            softKeyboard: SoftKeyboardVisibility,
            currentShift: () -> ShiftState,
        ) = ImeHardwareKeyHandler(
            currentShift = currentShift,
            dispatch = { action ->
                ImeKeyboardCallbackBinder.dispatch(session.actionHandler, session.suggestionService, action)
            },
            finish = session.actionHandler::finish,
            hotkeyEnabled = { softKeyboard.hotkeyEnabled },
            toggleSoftKeyboard = softKeyboard::toggle,
        )
    }
}
