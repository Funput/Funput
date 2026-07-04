package app.funput.funput.keyboard.interaction

import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.ShiftState

/** Coordinates touch-driven behaviors without coupling them to the Android view lifecycle. */
internal class KeyboardInteractionController(
    private val keySpec: (keyId: String) -> KeySpec?,
    onAction: (KeyAction) -> Unit,
    onVisualStateChanged: () -> Unit,
    schedule: (task: Runnable, delayMillis: Long) -> Unit,
    cancel: (task: Runnable) -> Unit,
    doubleTapTimeoutMillis: Long,
) {
    private val actionDispatcher = KeyboardActionDispatcher(
        keySpec = keySpec,
        onAction = onAction,
        onShiftStateChanged = onVisualStateChanged,
        doubleTapTimeoutMillis = doubleTapTimeoutMillis,
    )
    private val backspaceRepeat = BackspaceRepeatController(
        schedule = schedule,
        cancel = cancel,
        onRepeat = actionDispatcher::repeatBackspace,
    )

    val shiftState: ShiftState get() = actionDispatcher.shiftState

    fun onPointerKeyChanged(pointerId: Int, keyId: String?) {
        val isBackspace = keyId != null && keySpec(keyId)?.role == KeyRole.BACKSPACE
        backspaceRepeat.update(pointerId, isBackspace)
    }

    fun onKeyReleased(pointerId: Int, keyId: String?, eventTimeMillis: Long) {
        val isBackspace = keyId != null && keySpec(keyId)?.role == KeyRole.BACKSPACE
        if (!backspaceRepeat.finish(pointerId, isBackspace) && keyId != null) {
            actionDispatcher.dispatch(keyId, eventTimeMillis)
        }
    }

    fun cancel() = backspaceRepeat.cancelAll()

    fun reset() {
        backspaceRepeat.cancelAll()
        actionDispatcher.reset()
    }
}
