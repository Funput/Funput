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

    fun draw(
        canvas: Canvas,
        bitmap: Bitmap?,
        backgroundImage: KeyboardThemeBackgroundImage?,
        width: Int,
        height: Int,
    ) {
        if (bitmap == null || backgroundImage == null || width <= 0 || height <= 0) return
        paint.alpha = (backgroundImage.opacity * MaxAlpha).roundToInt().coerceIn(0, MaxAlpha)
        canvas.drawBitmap(bitmap, sourceRect(bitmap, width, height), RectF(0f, 0f, width.toFloat(), height.toFloat()), paint)
    }

    private fun sourceRect(bitmap: Bitmap, width: Int, height: Int): Rect {
        val targetRatio = width.toFloat() / height.toFloat()
        val bitmapRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
        return if (bitmapRatio > targetRatio) {
            val cropWidth = (bitmap.height * targetRatio).roundToInt()
            val left = (bitmap.width - cropWidth) / 2
            Rect(left, 0, left + cropWidth, bitmap.height)
        } else {
            val cropHeight = (bitmap.width / targetRatio).roundToInt()
            val top = (bitmap.height - cropHeight) / 2
            Rect(0, top, bitmap.width, top + cropHeight)
        }
    }
}

private const val MaxAlpha = 255
