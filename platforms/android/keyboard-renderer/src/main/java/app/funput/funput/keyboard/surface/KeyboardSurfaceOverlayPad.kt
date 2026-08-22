package app.funput.funput.keyboard.surface

import android.graphics.Canvas
import android.view.MotionEvent
import kotlin.math.ceil

/**
 * Grows a transparent strip above the keyboard so an alternate palette can sit
 * fully above a top-row key without covering it. Drawing and touches stay in
 * keyboard coordinates; the pad is only a viewport shift.
 */
internal class KeyboardSurfaceOverlayPad(
    private val requestLayout: () -> Unit,
    private val onPixelsChanged: (Int) -> Unit,
) {
    var pixels: Int = 0
        private set

    fun sync(overflowAbove: Float) {
        val next = ceil(overflowAbove.toDouble()).toInt().coerceAtLeast(0)
        if (pixels == next) return
        pixels = next
        onPixelsChanged(next)
        requestLayout()
    }

    fun keyboardHeight(viewHeight: Int): Int = (viewHeight - pixels).coerceAtLeast(0)

    fun drawTranslated(canvas: Canvas, draw: () -> Unit) {
        if (pixels == 0) {
            draw()
            return
        }
        val checkpoint = canvas.save()
        canvas.translate(0f, pixels.toFloat())
        draw()
        canvas.restoreToCount(checkpoint)
    }

    fun withKeyboardCoordinates(event: MotionEvent, block: () -> Boolean): Boolean {
        if (pixels == 0) return block()
        event.offsetLocation(0f, -pixels.toFloat())
        return try {
            block()
        } finally {
            event.offsetLocation(0f, pixels.toFloat())
        }
    }
}
