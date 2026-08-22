package app.funput.funput.ime.hardware

import app.funput.funput.ime.ImeEditingSession
import app.funput.funput.ime.ImeKeyboardCallbackBinder
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
) {
    private val consumedDown = mutableSetOf<Int>()

    fun onKeyDown(stroke: HardwareKeyStroke): Boolean = when (val decision = HardwareKeyActionMapper.map(stroke)) {
        is HardwareKeyDecision.Consume -> {
            dispatch(decision.action.applyHardwareCasing(currentShift()))
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
        fun bind(session: ImeEditingSession, currentShift: () -> ShiftState) = ImeHardwareKeyHandler(
            currentShift = currentShift,
            dispatch = { action ->
                ImeKeyboardCallbackBinder.dispatch(session.actionHandler, session.suggestionService, action)
            },
            finish = session.actionHandler::finish,
        )
    }
}
