package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
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
    private val chevronPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val chevronPath = Path()
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

        chevronPaint.color = (theme.secondaryLabelColor and 0x00FFFFFF) or ChevronAlpha
        val offset = min(key.bounds.width * 0.34f, metrics.dp(MaxChevronOffsetDp))
        val height = min(key.bounds.height * 0.22f, metrics.dp(MaxChevronHeightDp))
        drawFilledChevron(canvas, centerX - offset, centerY, height, pointsLeft = true)
        drawFilledChevron(canvas, centerX + offset, centerY, height, pointsLeft = false)
    }

    private fun drawFilledChevron(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        height: Float,
        pointsLeft: Boolean,
    ) {
        val halfHeight = height * 0.5f
        val width = height * ChevronWidthRatio
        val tipX = centerX + if (pointsLeft) -width * 0.5f else width * 0.5f
        val baseX = centerX + if (pointsLeft) width * 0.5f else -width * 0.5f
        chevronPath.reset()
        chevronPath.moveTo(tipX, centerY)
        chevronPath.lineTo(baseX, centerY - halfHeight)
        chevronPath.lineTo(baseX, centerY + halfHeight)
        chevronPath.close()
        canvas.drawPath(chevronPath, chevronPaint)
    }

    private companion object {
        const val LabelSizeSp = 12f
        const val MaxChevronOffsetDp = 68f
        const val MaxChevronHeightDp = 7f
        const val ChevronWidthRatio = 0.62f
        const val ChevronAlpha = 0x99000000.toInt()
    }
}
