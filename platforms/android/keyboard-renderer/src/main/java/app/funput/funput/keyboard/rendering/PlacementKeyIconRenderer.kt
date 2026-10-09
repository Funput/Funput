package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import app.funput.funput.keyboard.layout.KeyBounds
import kotlin.math.min

/** Shared placement symbol for toolbar keys and alternate palettes. */
internal class PlacementKeyIconRenderer(private val metrics: RenderMetrics) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val rect = RectF()

    fun draw(canvas: Canvas, bounds: KeyBounds, color: Int) {
        paint.color = color
        paint.strokeWidth = metrics.dp(1.7f)
        val halfWidth = min(bounds.width, bounds.height) * 0.21f
        val halfHeight = halfWidth * 0.62f
        val left = bounds.centerX - halfWidth
        val right = bounds.centerX + halfWidth
        val top = bounds.centerY - halfHeight
        val bottom = bounds.centerY + halfHeight
        rect.set(left, top, right, bottom)
        canvas.drawRoundRect(rect, metrics.dp(2f), metrics.dp(2f), paint)
        canvas.drawLine(left, bottom + metrics.dp(4f), right, bottom + metrics.dp(4f), paint)
        canvas.drawLine(bounds.centerX, bottom + metrics.dp(4f), bounds.centerX, bottom, paint)
    }
}
