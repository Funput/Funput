package app.funput.funput.keyboard.rendering

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import app.funput.funput.theme.KeyboardThemeBackgroundImage
import kotlin.math.roundToInt

internal class KeyboardBackgroundImageRenderer {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val destination = RectF()
    private val source = Rect()

    fun draw(
        canvas: Canvas,
        bitmap: Bitmap?,
        backgroundImage: KeyboardThemeBackgroundImage?,
        width: Int,
        height: Int,
    ) {
        if (bitmap == null || backgroundImage == null || width <= 0 || height <= 0) return
        paint.alpha = (backgroundImage.opacity * MaxAlpha).roundToInt().coerceIn(0, MaxAlpha)
        destination.set(0f, 0f, width.toFloat(), height.toFloat())
        val crop = BackgroundImageCrop.sourceRect(
            imageWidth = bitmap.width,
            imageHeight = bitmap.height,
            targetWidth = width,
            targetHeight = height,
            framing = backgroundImage,
        )
        source.set(crop.left, crop.top, crop.right, crop.bottom)
        canvas.drawBitmap(bitmap, source, destination, paint)
        // Drawn after the image and before the keys, which is what makes it a legibility wash
        // rather than a tint on the whole keyboard.
        if (backgroundImage.overlayColor ushr 24 != 0) {
            overlayPaint.color = backgroundImage.overlayColor
            canvas.drawRect(destination, overlayPaint)
        }
    }
}

private const val MaxAlpha = 255
