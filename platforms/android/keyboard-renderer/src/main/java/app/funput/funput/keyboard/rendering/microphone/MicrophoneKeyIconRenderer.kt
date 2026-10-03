package app.funput.funput.keyboard.rendering.microphone

import app.funput.funput.keyboard.rendering.RenderMetrics

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min

internal class MicrophoneKeyIconRenderer(private val metrics: RenderMetrics) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val rect = RectF()
    private var normalColor = 0
    private var activeColor = 0

    fun updateTheme(theme: KeyboardTheme) {
        normalColor = theme.specialLabelColor
        activeColor = theme.accentColor
        paint.strokeWidth = metrics.dp(1.7f)
    }

    fun draw(canvas: Canvas, key: ResolvedKey) {
        paint.color = if (key.active) activeColor else normalColor
        val unit = min(key.bounds.height * 0.27f, metrics.dp(10f))
        val x = key.bounds.centerX
        val y = key.bounds.centerY
        rect.set(x - unit * 0.38f, y - unit, x + unit * 0.38f, y + unit * 0.3f)
        canvas.drawRoundRect(rect, unit * 0.38f, unit * 0.38f, paint)
        rect.set(x - unit * 0.7f, y - unit * 0.35f, x + unit * 0.7f, y + unit * 0.7f)
        canvas.drawArc(rect, 0f, 180f, false, paint)
        canvas.drawLine(x, y + unit * 0.7f, x, y + unit * 1.1f, paint)
        canvas.drawLine(x - unit * 0.4f, y + unit * 1.1f, x + unit * 0.4f, y + unit * 1.1f, paint)
    }
}
