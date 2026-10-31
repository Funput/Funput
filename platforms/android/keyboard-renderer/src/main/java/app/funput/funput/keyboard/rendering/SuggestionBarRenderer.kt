package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.graphics.withSave
import app.funput.funput.keyboard.interaction.PressedKeyState
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.suggestion.SuggestionBandSegment
import app.funput.funput.keyboard.suggestion.SuggestionBandSegment.Tone
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.LocalKeyboardThemeCatalog

/** Draws a row of suggestion-band slots: the candidates, or the actions a long press opens. */
internal class SuggestionBarRenderer(private val metrics: RenderMetrics) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val drawingRect = RectF()
    private val clipPath = Path()
    private val fontMetrics = Paint.FontMetrics()
    private var theme: KeyboardTheme = LocalKeyboardThemeCatalog.defaultTheme.theme

    fun updateTheme(theme: KeyboardTheme) {
        this.theme = theme
        dividerPaint.color = theme.suggestionDividerColor
    }

    fun draw(
        canvas: Canvas,
        bounds: KeyBounds,
        segments: List<SuggestionBandSegment>,
        pressedTargets: PressedKeyState,
    ) {
        if (segments.isEmpty()) return
        drawPressedSegments(canvas, bounds, segments, pressedTargets)
        segments.forEachIndexed { index, segment ->
            if (index > 0) drawDivider(canvas, bounds, segment.left)
            labelPaint.color = when (segment.tone) {
                // The pick the bar recommends (a repair, else the top suggestion) is accented.
                Tone.ACCENT -> theme.suggestionHighlightColor
                Tone.MUTED -> theme.secondaryLabelColor
                Tone.PLAIN -> theme.labelColor
            }
            val label = fitLabel(segment.title, segment.width - metrics.dp(LabelHorizontalInsetDp * 2f))
            // Per label, since a fitted one is smaller and has to stay centred in the band.
            labelPaint.getFontMetrics(fontMetrics)
            val baseline = bounds.centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(label, segment.centerX, baseline, labelPaint)
        }
    }

    /** Sizes [labelPaint] for [suggestion] and returns the text to draw; see [SuggestionLabelFit]. */
    private fun fitLabel(suggestion: String, availableWidth: Float): String {
        labelPaint.textSize = metrics.sp(SuggestionLabelSizeSp)
        labelPaint.textScaleX = 1f
        val fit = SuggestionLabelFit.fit(labelPaint.measureText(suggestion), availableWidth)
        labelPaint.textSize *= fit.sizeFactor
        labelPaint.textScaleX = fit.scaleX
        if (!fit.ellipsize) return suggestion
        return TextUtils.ellipsize(suggestion, labelPaint, availableWidth, TextUtils.TruncateAt.MIDDLE).toString()
    }

    private fun drawPressedSegments(
        canvas: Canvas,
        bounds: KeyBounds,
        segments: List<SuggestionBandSegment>,
        pressedTargets: PressedKeyState,
    ) {
        val pressed = segments.filter { it.pressable && pressedTargets.isPressed(it.targetId) }
        if (pressed.isEmpty()) return
        val radius = metrics.dp(theme.keyCornerRadiusDp)
        canvas.withSave {
            drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
            clipPath.reset()
            clipPath.addRoundRect(drawingRect, radius, radius, Path.Direction.CW)
            clipPath(clipPath)
            fillPaint.color = theme.pressedKeyColor
            pressed.forEach { drawRect(it.left, bounds.top, it.right, bounds.bottom, fillPaint) }
        }
    }

    private fun drawDivider(canvas: Canvas, bounds: KeyBounds, x: Float) {
        // Taken from the band rather than in dp so the divider keeps its proportions at
        // every keyboard size setting instead of swallowing a short band.
        val inset = bounds.height * DividerInsetRatio
        canvas.drawLine(x, bounds.top + inset, x, bounds.bottom - inset, dividerPaint)
    }

    private companion object {
        const val SuggestionLabelSizeSp = 16f
        const val DividerInsetRatio = 0.22f
        const val LabelHorizontalInsetDp = 8f
    }
}
