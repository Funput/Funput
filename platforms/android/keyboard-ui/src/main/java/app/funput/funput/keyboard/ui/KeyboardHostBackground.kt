package app.funput.funput.keyboard.ui

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import app.funput.funput.keyboard.KeyboardSurfaceView

/**
 * Fills the keyboard host except the transparent overlay strip used to draw an
 * alternate palette above a top-row key without covering the app with theme color.
 */
internal class KeyboardHostBackground : Drawable() {
    private val paint = Paint()
    var color: Int
        get() = paint.color
        set(value) {
            if (paint.color == value) return
            paint.color = value
            invalidateSelf()
        }
    var overlayPadTop: Int = 0
        set(value) {
            if (field == value) return
            field = value
            invalidateSelf()
        }

    override fun draw(canvas: Canvas) {
        val bounds = bounds
        canvas.drawRect(
            bounds.left.toFloat(),
            (bounds.top + overlayPadTop).toFloat(),
            bounds.right.toFloat(),
            bounds.bottom.toFloat(),
            paint,
        )
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.OPAQUE

    fun attach(surface: KeyboardSurfaceView, requestLayout: () -> Unit) {
        color = surface.keyboardTheme.backgroundEndColor
        surface.onOverlayPadChanged = { overlayPadTop = it; requestLayout() }
    }
}
