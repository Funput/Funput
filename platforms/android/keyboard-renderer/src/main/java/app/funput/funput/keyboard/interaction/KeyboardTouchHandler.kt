package app.funput.funput.keyboard.interaction

import android.view.MotionEvent

/** Converts Android pointer events into renderer-independent pressed-key state. */
internal class KeyboardTouchHandler(
    private val keyAt: (x: Float, y: Float) -> String?,
    private val onPressedStateChanged: () -> Unit,
    private val requestParentIntercept: (disallow: Boolean) -> Unit,
) : PressedKeyState {
    private val pressedKeys = PressedKeyTracker()

    fun onTouchEvent(event: MotionEvent): Result = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> handleDown(event)
        MotionEvent.ACTION_POINTER_DOWN -> {
            update(event, event.actionIndex)
            Result.HANDLED
        }
        MotionEvent.ACTION_MOVE -> {
            repeat(event.pointerCount) { pointerIndex -> update(event, pointerIndex) }
            Result.HANDLED
        }
        MotionEvent.ACTION_POINTER_UP -> {
            release(event.getPointerId(event.actionIndex))
            Result.HANDLED
        }
        MotionEvent.ACTION_UP -> handleUp(event)
        MotionEvent.ACTION_CANCEL -> {
            clear()
            requestParentIntercept(false)
            Result.HANDLED
        }
        else -> Result.UNHANDLED
    }

    override fun isPressed(keyId: String): Boolean = pressedKeys.isPressed(keyId)

    fun clear() {
        if (pressedKeys.clear()) onPressedStateChanged()
    }

    private fun handleDown(event: MotionEvent): Result {
        val handled = update(event, event.actionIndex)
        if (handled) requestParentIntercept(true)
        return if (handled) Result.HANDLED else Result.UNHANDLED
    }

    private fun handleUp(event: MotionEvent): Result {
        val pointerId = event.getPointerId(event.actionIndex)
        val wasPressed = pressedKeys.keyForPointer(pointerId) != null
        release(pointerId)
        requestParentIntercept(false)
        return if (wasPressed) Result.CLICK else Result.HANDLED
    }

    private fun update(event: MotionEvent, pointerIndex: Int): Boolean {
        val keyId = keyAt(event.getX(pointerIndex), event.getY(pointerIndex))
        if (pressedKeys.update(event.getPointerId(pointerIndex), keyId)) {
            onPressedStateChanged()
        }
        return keyId != null
    }

    private fun release(pointerId: Int) {
        if (pressedKeys.release(pointerId)) onPressedStateChanged()
    }

    enum class Result {
        UNHANDLED,
        HANDLED,
        CLICK,
    }
}
