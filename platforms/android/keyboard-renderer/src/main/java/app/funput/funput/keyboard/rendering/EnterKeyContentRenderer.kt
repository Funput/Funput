package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyboardEnterAction
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min

/** Draws the action advertised by the editor in the Enter-key position. */
internal class EnterKeyContentRenderer(private val metrics: RenderMetrics) {
    private val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val path = Path()
    private val fontMetrics = Paint.FontMetrics()

    fun updateTheme(theme: KeyboardTheme) {
        iconPaint.color = theme.accentColor
        iconPaint.strokeWidth = metrics.dp(1.7f)
        labelPaint.color = theme.accentColor
    }

    fun draw(canvas: Canvas, key: ResolvedKey, action: KeyboardEnterAction) {
        when (action) {
            is KeyboardEnterAction.Custom -> drawLabel(canvas, key, action.label)
            KeyboardEnterAction.Standard.NEW_LINE -> drawNewLine(canvas, key)
            KeyboardEnterAction.Standard.SEARCH -> drawSearch(canvas, key)
            KeyboardEnterAction.Standard.SEND -> drawSend(canvas, key)
            KeyboardEnterAction.Standard.DONE -> drawDone(canvas, key)
            KeyboardEnterAction.Standard.GO -> drawArrow(canvas, key, direction = 1f, hasBar = false)
            KeyboardEnterAction.Standard.NEXT -> drawArrow(canvas, key, direction = 1f, hasBar = true)
            KeyboardEnterAction.Standard.PREVIOUS -> drawArrow(canvas, key, direction = -1f, hasBar = true)
        }
    }

    private fun drawNewLine(canvas: Canvas, key: ResolvedKey) {
        val size = iconSize(key)
        val right = key.bounds.centerX + size * 0.5f
        val left = key.bounds.centerX - size * 0.5f
        val centerY = key.bounds.centerY
        path.reset()
        path.moveTo(right, centerY - size * 0.48f)
        path.lineTo(right, centerY + size * 0.18f)
        path.quadTo(right, centerY + size * 0.45f, right - size * 0.28f, centerY + size * 0.45f)
        path.lineTo(left, centerY + size * 0.45f)
        path.moveTo(left, centerY + size * 0.45f)
        path.lineTo(left + size * 0.3f, centerY + size * 0.15f)
        path.moveTo(left, centerY + size * 0.45f)
        path.lineTo(left + size * 0.3f, centerY + size * 0.75f)
        canvas.drawPath(path, iconPaint)
    }

    private fun drawSearch(canvas: Canvas, key: ResolvedKey) {
        val size = iconSize(key)
        val radius = size * 0.3f
        val centerX = key.bounds.centerX - size * 0.1f
        val centerY = key.bounds.centerY - size * 0.1f
        canvas.drawCircle(centerX, centerY, radius, iconPaint)
        canvas.drawLine(
            centerX + radius * 0.72f,
            centerY + radius * 0.72f,
            centerX + radius * 1.55f,
            centerY + radius * 1.55f,
            iconPaint,
        )
    }

    private fun drawSend(canvas: Canvas, key: ResolvedKey) {
        val size = iconSize(key)
        val left = key.bounds.centerX - size * 0.58f
        val right = key.bounds.centerX + size * 0.58f
        val top = key.bounds.centerY - size * 0.48f
        val bottom = key.bounds.centerY + size * 0.48f
        path.reset()
        path.moveTo(left, top)
        path.lineTo(right, key.bounds.centerY)
        path.lineTo(left, bottom)
        path.lineTo(left + size * 0.2f, key.bounds.centerY)
        path.close()
        canvas.drawPath(path, iconPaint)
        canvas.drawLine(left + size * 0.2f, key.bounds.centerY, right, key.bounds.centerY, iconPaint)
    }

    private fun drawDone(canvas: Canvas, key: ResolvedKey) {
        val size = iconSize(key)
        path.reset()
        path.moveTo(key.bounds.centerX - size * 0.55f, key.bounds.centerY)
        path.lineTo(key.bounds.centerX - size * 0.12f, key.bounds.centerY + size * 0.38f)
        path.lineTo(key.bounds.centerX + size * 0.58f, key.bounds.centerY - size * 0.42f)
        canvas.drawPath(path, iconPaint)
    }

    private fun drawArrow(canvas: Canvas, key: ResolvedKey, direction: Float, hasBar: Boolean) {
        val size = iconSize(key)
        val startX = key.bounds.centerX - direction * size * 0.5f
        val endX = key.bounds.centerX + direction * size * 0.5f
        canvas.drawLine(startX, key.bounds.centerY, endX, key.bounds.centerY, iconPaint)
        canvas.drawLine(endX, key.bounds.centerY, endX - direction * size * 0.32f, key.bounds.centerY - size * 0.3f, iconPaint)
        canvas.drawLine(endX, key.bounds.centerY, endX - direction * size * 0.32f, key.bounds.centerY + size * 0.3f, iconPaint)
        if (hasBar) {
            val barX = endX + direction * size * 0.18f
            canvas.drawLine(barX, key.bounds.centerY - size * 0.38f, barX, key.bounds.centerY + size * 0.38f, iconPaint)
        }
    }

    private fun drawLabel(canvas: Canvas, key: ResolvedKey, label: String) {
        labelPaint.textSize = metrics.sp(12f)
        val availableWidth = key.bounds.width - metrics.dp(8f)
        val measuredWidth = labelPaint.measureText(label)
        if (measuredWidth > availableWidth) labelPaint.textSize *= availableWidth / measuredWidth
        labelPaint.getFontMetrics(fontMetrics)
        val baseline = key.bounds.centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(label, key.bounds.centerX, baseline, labelPaint)
    }

    private fun iconSize(key: ResolvedKey) = min(key.bounds.width, key.bounds.height) * 0.36f
}
