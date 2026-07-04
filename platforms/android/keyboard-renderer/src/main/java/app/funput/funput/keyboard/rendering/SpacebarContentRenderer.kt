package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyboardLanguage
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.min

/** Draws the current language and bidirectional swipe affordances inside Spacebar. */
internal class SpacebarContentRenderer(private val metrics: RenderMetrics) {
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val arrowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fontMetrics = Paint.FontMetrics()

    fun draw(
        canvas: Canvas,
        key: ResolvedKey,
        theme: KeyboardTheme,
        language: KeyboardLanguage,
    ) {
        labelPaint.color = theme.secondaryLabelColor
        labelPaint.textSize = metrics.sp(LabelSizeSp)
        labelPaint.getFontMetrics(fontMetrics)
        val centerX = key.bounds.centerX
        val centerY = key.bounds.centerY
        val baseline = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(language.displayLabel, centerX, baseline, labelPaint)

        arrowPaint.color = theme.accentColor
        arrowPaint.strokeWidth = metrics.dp(ArrowStrokeWidthDp)
        val offset = min(key.bounds.width * 0.36f, metrics.dp(MaxArrowOffsetDp))
        val size = min(key.bounds.height * 0.13f, metrics.dp(MaxArrowSizeDp))
        drawChevron(canvas, centerX - offset, centerY, size, pointsRight = false)
        drawChevron(canvas, centerX + offset, centerY, size, pointsRight = true)
    }

    private fun drawChevron(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        size: Float,
        pointsRight: Boolean,
    ) {
        val direction = if (pointsRight) 1f else -1f
        val tipX = centerX + size * direction
        val tailX = centerX - size * direction
        canvas.drawLine(tailX, centerY - size, tipX, centerY, arrowPaint)
        canvas.drawLine(tipX, centerY, tailX, centerY + size, arrowPaint)
    }

    private companion object {
        const val LabelSizeSp = 12f
        const val ArrowStrokeWidthDp = 1.5f
        const val MaxArrowOffsetDp = 72f
        const val MaxArrowSizeDp = 5f
    }
}
