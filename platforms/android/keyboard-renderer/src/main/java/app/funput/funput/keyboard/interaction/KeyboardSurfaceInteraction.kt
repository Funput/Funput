package app.funput.funput.keyboard.interaction

import android.view.MotionEvent
import app.funput.funput.keyboard.model.KeyAction
import app.funput.funput.keyboard.model.KeySpec
import app.funput.funput.keyboard.model.ShiftState

/** Wires Android touch routing to renderer-independent keyboard interaction state. */
internal class KeyboardSurfaceInteraction(
    keyAt: (x: Float, y: Float) -> String?,
    keySpec: (keyId: String) -> KeySpec?,
    onAction: (KeyAction) -> Unit,
    onVisualStateChanged: () -> Unit,
    schedule: (task: Runnable, delayMillis: Long) -> Unit,
    cancel: (task: Runnable) -> Unit,
    requestParentIntercept: (disallow: Boolean) -> Unit,
    doubleTapTimeoutMillis: Long,
) : PressedKeyState {
    private val controller = KeyboardInteractionController(
        keySpec = keySpec,
        onAction = onAction,
        onVisualStateChanged = onVisualStateChanged,
        schedule = schedule,
        cancel = cancel,
        doubleTapTimeoutMillis = doubleTapTimeoutMillis,
    )
    private val touchHandler = KeyboardTouchHandler(
        keyAt = keyAt,
        onPressedStateChanged = onVisualStateChanged,
        onPointerKeyChanged = controller::onPointerKeyChanged,
        onKeyReleased = controller::onKeyReleased,
        onCancelled = controller::cancel,
        requestParentIntercept = requestParentIntercept,
    )

    val shiftState: ShiftState get() = controller.shiftState

    fun onTouchEvent(event: MotionEvent): KeyboardTouchHandler.Result = touchHandler.onTouchEvent(event)

    fun clear() = touchHandler.clear()

    fun reset() {
        touchHandler.clear()
        controller.reset()
    }

    override fun isPressed(keyId: String): Boolean = touchHandler.isPressed(keyId)
}
