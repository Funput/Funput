package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min

internal class ClipboardKeyIconRenderer(private val metrics: RenderMetrics) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val body = RectF()
    private val clip = RectF()

    fun updateTheme(theme: KeyboardTheme) {
        paint.color = theme.specialLabelColor
        paint.strokeWidth = metrics.dp(1.7f)
    }

    fun draw(canvas: Canvas, key: ResolvedKey) {
        val iconHeight = min(
            min(key.bounds.width, key.bounds.height) * IconSizeRatio,
            metrics.dp(MaxIconHeightDp),
        )
        val height = iconHeight / IconEnvelopeRatio
        val width = height * 0.76f
        body.set(
            key.bounds.centerX - width / 2f,
            key.bounds.centerY - height * 0.42f,
            key.bounds.centerX + width / 2f,
            key.bounds.centerY + height / 2f,
        )
        canvas.drawRoundRect(body, metrics.dp(2.5f), metrics.dp(2.5f), paint)
        val clipWidth = width * 0.52f
        clip.set(
            key.bounds.centerX - clipWidth / 2f,
            body.top - height * 0.12f,
            key.bounds.centerX + clipWidth / 2f,
            body.top + height * 0.18f,
        )
        canvas.drawRoundRect(clip, metrics.dp(2f), metrics.dp(2f), paint)
    }

    private companion object {
        const val IconSizeRatio = 0.42f
        const val IconEnvelopeRatio = 1.04f
        const val MaxIconHeightDp = 19f
    }
}
