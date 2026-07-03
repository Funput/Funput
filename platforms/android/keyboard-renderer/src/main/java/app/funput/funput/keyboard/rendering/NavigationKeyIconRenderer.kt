package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min

internal class NavigationKeyIconRenderer(private val metrics: RenderMetrics) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val path = Path()

    fun updateTheme(theme: KeyboardTheme) {
        paint.color = theme.labelColor
        paint.strokeWidth = metrics.dp(1.7f)
    }

    fun drawShift(canvas: Canvas, key: ResolvedKey) {
        val size = min(key.bounds.width, key.bounds.height) * 0.38f
        val centerX = key.bounds.centerX
        val centerY = key.bounds.centerY
        val half = size / 2f
        path.reset()
        path.moveTo(centerX, centerY - half)
        path.lineTo(centerX + half, centerY)
        path.lineTo(centerX + half * 0.42f, centerY)
        path.lineTo(centerX + half * 0.42f, centerY + half)
        path.lineTo(centerX - half * 0.42f, centerY + half)
        path.lineTo(centerX - half * 0.42f, centerY)
        path.lineTo(centerX - half, centerY)
        path.close()
        canvas.drawPath(path, paint)
    }

    fun drawEnter(canvas: Canvas, key: ResolvedKey) {
        val size = min(key.bounds.width, key.bounds.height) * 0.34f
        val centerX = key.bounds.centerX
        val centerY = key.bounds.centerY
        val right = centerX + size * 0.5f
        val left = centerX - size * 0.5f
        path.reset()
        path.moveTo(right, centerY - size * 0.48f)
        path.lineTo(right, centerY + size * 0.18f)
        path.quadTo(right, centerY + size * 0.45f, right - size * 0.28f, centerY + size * 0.45f)
        path.lineTo(left, centerY + size * 0.45f)
        path.moveTo(left, centerY + size * 0.45f)
        path.lineTo(left + size * 0.3f, centerY + size * 0.15f)
        path.moveTo(left, centerY + size * 0.45f)
        path.lineTo(left + size * 0.3f, centerY + size * 0.75f)
        canvas.drawPath(path, paint)
    }
}
