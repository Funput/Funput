package app.funput.funput.keyboard.interaction

import android.view.MotionEvent

/** Converts Android pointer events into renderer-independent pressed-key state. */
internal class KeyboardTouchHandler(
    keyAt: (x: Float, y: Float) -> String?,
    onPressedStateChanged: () -> Unit,
    private val onPointerStarted: (pointerId: Int, keyId: String?, x: Float, y: Float) -> Unit,
    private val onPointerKeyChanged: (pointerId: Int, keyId: String?) -> Unit,
    private val onPointerMoved: (pointerId: Int, keyId: String?, x: Float, y: Float) -> Unit,
    private val onKeyReleased: (pointerId: Int, keyId: String?, x: Float, y: Float, eventTimeMillis: Long) -> Unit,
    private val isPointerCaptured: (pointerId: Int) -> Boolean,
    private val onCancelled: () -> Unit,
    private val requestParentIntercept: (disallow: Boolean) -> Unit,
) : PressedKeyState {
    private val pointerSession = PointerKeySession(keyAt, onPressedStateChanged)

    fun onTouchEvent(event: MotionEvent): Result = when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> handleDown(event)
        MotionEvent.ACTION_POINTER_DOWN -> {
            startPointer(event, event.actionIndex)
            Result.HANDLED
        }
        MotionEvent.ACTION_MOVE -> {
            repeat(event.pointerCount) { pointerIndex -> update(event, pointerIndex) }
            Result.HANDLED
        }
        MotionEvent.ACTION_POINTER_UP -> {
            release(event, event.actionIndex)
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

    fun clear() {
        pointerSession.clear()
        onCancelled()
    }

    fun capture(pointerId: Int) = pointerSession.suspend(pointerId)

    private fun handleDown(event: MotionEvent): Result {
        val handled = startPointer(event, event.actionIndex)
        if (handled) requestParentIntercept(true)
        return if (handled) Result.HANDLED else Result.UNHANDLED
    }

    private fun handleUp(event: MotionEvent): Result {
        val releasedKeyId = release(event, event.actionIndex)
        requestParentIntercept(false)
        return if (releasedKeyId != null) Result.CLICK else Result.HANDLED
    }

    private fun update(event: MotionEvent, pointerIndex: Int): Boolean {
        val pointerId = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex)
        val y = event.getY(pointerIndex)
        if (isPointerCaptured(pointerId)) {
            onPointerMoved(pointerId, null, x, y)
            return true
        }
        val previousKeyId = pointerSession.keyForPointer(pointerId)
        val handled = pointerSession.update(
            pointerId = pointerId,
            x = x,
            y = y,
        )
        val currentKeyId = pointerSession.keyForPointer(pointerId)
        if (previousKeyId != currentKeyId) onPointerKeyChanged(pointerId, currentKeyId)
        onPointerMoved(pointerId, currentKeyId, x, y)
        return handled
    }

    private fun startPointer(event: MotionEvent, pointerIndex: Int): Boolean {
        val handled = update(event, pointerIndex)
        val pointerId = event.getPointerId(pointerIndex)
        onPointerStarted(
            pointerId,
            pointerSession.keyForPointer(pointerId),
            event.getX(pointerIndex),
            event.getY(pointerIndex),
        )
        return handled
    }

    private fun release(event: MotionEvent, pointerIndex: Int): String? {
        val keyId = pointerSession.release(
            pointerId = event.getPointerId(pointerIndex),
            x = event.getX(pointerIndex),
            y = event.getY(pointerIndex),
        )
        onKeyReleased(
            event.getPointerId(pointerIndex),
            keyId,
            event.getX(pointerIndex),
            event.getY(pointerIndex),
            event.eventTime,
        )
        return keyId
    }

    enum class Result {
        UNHANDLED,
        HANDLED,
        CLICK,
    }
}
