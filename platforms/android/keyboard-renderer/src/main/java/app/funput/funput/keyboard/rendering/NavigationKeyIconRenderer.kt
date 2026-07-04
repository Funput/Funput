package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.ShiftState
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

    fun drawShift(canvas: Canvas, key: ResolvedKey, shiftState: ShiftState) {
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
        paint.style = if (shiftState.isActive) Paint.Style.FILL else Paint.Style.STROKE
        canvas.drawPath(path, paint)
        paint.style = Paint.Style.STROKE
        if (shiftState == ShiftState.CAPS_LOCK) {
            val indicatorY = centerY + half * 1.35f
            canvas.drawLine(centerX - half * 0.45f, indicatorY, centerX + half * 0.45f, indicatorY, paint)
        }
    }

}
