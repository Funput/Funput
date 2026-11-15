package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min

/** Two stacked frames (copy) with an accent badge when clipboard content is offered. */
internal class ClipboardKeyIconRenderer(private val metrics: RenderMetrics) {
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val back = RectF()
    private val front = RectF()
    private var accentColor: Int = 0

    fun updateTheme(theme: KeyboardTheme) {
        stroke.color = theme.specialLabelColor
        stroke.strokeWidth = metrics.dp(1.7f)
        accentColor = theme.accentColor
    }

    fun draw(canvas: Canvas, key: ResolvedKey, offerActive: Boolean) {
        val size = min(
            min(key.bounds.width, key.bounds.height) * IconSizeRatio,
            metrics.dp(MaxIconHeightDp),
        )
        val offset = size * StackOffsetRatio
        val frame = size - offset
        val radius = metrics.dp(CornerRadiusDp)
        val left = key.bounds.centerX - size / 2f
        val top = key.bounds.centerY - size / 2f
        back.set(left + offset, top, left + offset + frame, top + frame)
        front.set(left, top + offset, left + frame, top + offset + frame)
        canvas.drawRoundRect(back, radius, radius, stroke)
        canvas.drawRoundRect(front, radius, radius, stroke)
        if (offerActive) drawBadge(canvas, key)
    }

    private fun drawBadge(canvas: Canvas, key: ResolvedKey) {
        val radius = metrics.dp(BadgeRadiusDp)
        fill.color = accentColor
        canvas.drawCircle(
            key.bounds.right - metrics.dp(BadgeInsetDp) - radius,
            key.bounds.top + metrics.dp(BadgeInsetDp) + radius,
            radius,
            fill,
        )
    }

    private companion object {
        const val IconSizeRatio = 0.42f
        const val MaxIconHeightDp = 19f
        const val StackOffsetRatio = 0.28f
        const val CornerRadiusDp = 2f
        const val BadgeRadiusDp = 2.25f
        const val BadgeInsetDp = 5f
    }
}
