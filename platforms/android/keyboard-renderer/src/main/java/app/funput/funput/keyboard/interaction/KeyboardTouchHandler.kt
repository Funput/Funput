package app.funput.funput.keyboard.interaction

import android.view.MotionEvent

/** Converts Android pointer events into renderer-independent pressed-key state. */
internal class KeyboardTouchHandler(
    keyAt: (x: Float, y: Float) -> String?,
    onPressedStateChanged: () -> Unit,
    private val requestParentIntercept: (disallow: Boolean) -> Unit,
) : PressedKeyState {
    private val pointerSession = PointerKeySession(keyAt, onPressedStateChanged)

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

    override fun isPressed(keyId: String): Boolean = pointerSession.isPressed(keyId)

    fun clear() = pointerSession.clear()

    private fun handleDown(event: MotionEvent): Result {
        val handled = update(event, event.actionIndex)
        if (handled) requestParentIntercept(true)
        return if (handled) Result.HANDLED else Result.UNHANDLED
    }

    private fun handleUp(event: MotionEvent): Result {
        val pointerId = event.getPointerId(event.actionIndex)
        val wasPressed = pointerSession.release(pointerId)
        requestParentIntercept(false)
        return if (wasPressed) Result.CLICK else Result.HANDLED
    }

    private fun update(event: MotionEvent, pointerIndex: Int): Boolean {
        return pointerSession.update(
            pointerId = event.getPointerId(pointerIndex),
            x = event.getX(pointerIndex),
            y = event.getY(pointerIndex),
        )
    }

    private fun release(pointerId: Int) {
        pointerSession.release(pointerId)
    }

    enum class Result {
        UNHANDLED,
        HANDLED,
        CLICK,
    }
}
