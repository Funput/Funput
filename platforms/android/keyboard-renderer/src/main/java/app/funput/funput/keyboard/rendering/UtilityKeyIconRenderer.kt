package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min

internal class UtilityKeyIconRenderer(private val metrics: RenderMetrics) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()
    private val drawingRect = RectF()

    fun updateTheme(theme: KeyboardTheme) {
        paint.color = theme.specialLabelColor
        paint.strokeWidth = metrics.dp(1.7f)
    }

    fun drawBackspace(canvas: Canvas, key: ResolvedKey) {
        val iconWidth = min(key.bounds.width * 0.46f, metrics.dp(25f))
        val iconHeight = iconWidth * 0.62f
        val left = key.bounds.centerX - iconWidth / 2f
        val right = key.bounds.centerX + iconWidth / 2f
        val top = key.bounds.centerY - iconHeight / 2f
        val bottom = key.bounds.centerY + iconHeight / 2f
        path.reset()
        path.moveTo(left, key.bounds.centerY)
        path.lineTo(left + iconHeight * 0.45f, top)
        path.lineTo(right, top)
        path.lineTo(right, bottom)
        path.lineTo(left + iconHeight * 0.45f, bottom)
        path.close()
        canvas.drawPath(path, paint)

        val crossCenter = right - iconHeight * 0.48f
        val crossRadius = iconHeight * 0.18f
        canvas.drawLine(
            crossCenter - crossRadius,
            key.bounds.centerY - crossRadius,
            crossCenter + crossRadius,
            key.bounds.centerY + crossRadius,
            paint,
        )
        canvas.drawLine(
            crossCenter + crossRadius,
            key.bounds.centerY - crossRadius,
            crossCenter - crossRadius,
            key.bounds.centerY + crossRadius,
            paint,
        )
    }

    fun drawSystemInputMethod(canvas: Canvas, key: ResolvedKey) {
        val radius = min(key.bounds.width, key.bounds.height) * 0.2f
        val centerX = key.bounds.centerX
        val centerY = key.bounds.centerY
        drawingRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius)
        canvas.drawOval(drawingRect, paint)
        canvas.drawOval(
            centerX - radius * 0.45f,
            centerY - radius,
            centerX + radius * 0.45f,
            centerY + radius,
            paint,
        )
        canvas.drawLine(centerX - radius, centerY, centerX + radius, centerY, paint)
    }

    fun drawEmoji(canvas: Canvas, key: ResolvedKey) {
        val radius = min(key.bounds.width, key.bounds.height) * 0.19f
        drawingRect.set(
            key.bounds.centerX - radius,
            key.bounds.centerY - radius,
            key.bounds.centerX + radius,
            key.bounds.centerY + radius,
        )
        canvas.drawOval(drawingRect, paint)

        val eyeRadius = radius * 0.07f
        paint.style = Paint.Style.FILL
        canvas.drawCircle(
            key.bounds.centerX - radius * 0.34f,
            key.bounds.centerY - radius * 0.24f,
            eyeRadius,
            paint,
        )
        canvas.drawCircle(
            key.bounds.centerX + radius * 0.34f,
            key.bounds.centerY - radius * 0.24f,
            eyeRadius,
            paint,
        )
        paint.style = Paint.Style.STROKE
        drawingRect.set(
            key.bounds.centerX - radius * 0.48f,
            key.bounds.centerY - radius * 0.06f,
            key.bounds.centerX + radius * 0.48f,
            key.bounds.centerY + radius * 0.5f,
        )
        canvas.drawArc(drawingRect, 18f, 144f, false, paint)
    }
}
