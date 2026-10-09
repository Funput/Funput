package app.funput.funput.keyboard.popover.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.popover.model.KeyAlternate
import app.funput.funput.keyboard.rendering.PlacementKeyIconRenderer
import app.funput.funput.keyboard.rendering.RenderMetrics
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

internal class AlternateActionIconRenderer(private val metrics: RenderMetrics) {
    private val placement = PlacementKeyIconRenderer(metrics)
    private val gear = Path()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    fun draw(canvas: Canvas, bounds: KeyBounds, action: KeyAlternate.Action, color: Int) {
        when (action) {
            KeyAlternate.Action.PLACEMENT -> placement.draw(canvas, bounds, color)
            KeyAlternate.Action.SETTINGS -> drawGear(canvas, bounds, color)
        }
    }

    private fun drawGear(canvas: Canvas, bounds: KeyBounds, color: Int) {
        paint.color = color
        paint.strokeWidth = metrics.dp(1.7f)
        val radius = min(bounds.width, bounds.height) * 0.25f
        gear.reset()
        repeat(32) { index ->
            val angle = index * Math.PI / 16.0
            val reach = radius * if (index % 4 in 1..2) 1f else 0.77f
            val x = bounds.centerX + cos(angle).toFloat() * reach
            val y = bounds.centerY + sin(angle).toFloat() * reach
            if (index == 0) gear.moveTo(x, y) else gear.lineTo(x, y)
        }
        gear.close()
        canvas.drawPath(gear, paint)
        canvas.drawCircle(bounds.centerX, bounds.centerY, radius * 0.32f, paint)
    }
}
