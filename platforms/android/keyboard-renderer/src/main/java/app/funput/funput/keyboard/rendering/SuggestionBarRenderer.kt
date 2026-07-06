package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.withSave
import app.funput.funput.keyboard.interaction.PressedKeyState
import app.funput.funput.keyboard.interaction.SuggestionTargetIds
import app.funput.funput.keyboard.layout.KeyBounds
import app.funput.funput.keyboard.layout.ResolvedSuggestionBar
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.KeyboardThemeCatalog

internal class SuggestionBarRenderer(private val metrics: RenderMetrics) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val drawingRect = RectF()
    private val clipPath = Path()
    private val fontMetrics = Paint.FontMetrics()
    private var theme: KeyboardTheme = KeyboardThemeCatalog.default()

    fun updateTheme(theme: KeyboardTheme) {
        this.theme = theme
        fillPaint.color = theme.keyColor
        borderPaint.color = theme.keyBorderColor
        borderPaint.strokeWidth = metrics.dp(theme.keyBorderWidthDp)
        dividerPaint.color = theme.secondaryLabelColor and 0x00FFFFFF or DividerAlpha
        labelPaint.color = theme.labelColor
    }

    fun draw(
        canvas: Canvas,
        suggestionBar: ResolvedSuggestionBar,
        suggestions: List<String>,
        pressedTargets: PressedKeyState,
    ) {
        val bounds = suggestionBar.suggestionsBounds
        val radius = metrics.dp(theme.keyCornerRadiusDp)
        drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        canvas.drawRoundRect(drawingRect, radius, radius, fillPaint)
        if (theme.keyBorderWidthDp > 0f) {
            canvas.drawRoundRect(drawingRect, radius, radius, borderPaint)
        }
        if (suggestions.isEmpty()) return

        drawPressedSegments(canvas, bounds, radius, suggestions.size, pressedTargets)
        labelPaint.textSize = metrics.sp(SuggestionLabelSizeSp)
        labelPaint.getFontMetrics(fontMetrics)
        val segmentWidth = bounds.width / suggestions.size
        val baseline = bounds.centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        suggestions.forEachIndexed { index, suggestion ->
            if (index > 0) drawDivider(canvas, bounds, bounds.left + segmentWidth * index)
            canvas.drawText(
                suggestion,
                bounds.left + segmentWidth * (index + 0.5f),
                baseline,
                labelPaint,
            )
        }
    }

    private fun drawPressedSegments(
        canvas: Canvas,
        bounds: KeyBounds,
        radius: Float,
        suggestionCount: Int,
        pressedTargets: PressedKeyState,
    ) {
        val segmentWidth = bounds.width / suggestionCount
        canvas.withSave {
            drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
            clipPath.reset()
            clipPath.addRoundRect(drawingRect, radius, radius, Path.Direction.CW)
            clipPath(clipPath)
            fillPaint.color = theme.pressedKeyColor
            repeat(suggestionCount) { index ->
                if (pressedTargets.isPressed(SuggestionTargetIds.id(index))) {
                    val left = bounds.left + segmentWidth * index
                    drawRect(left, bounds.top, left + segmentWidth, bounds.bottom, fillPaint)
                }
            }
        }
        fillPaint.color = theme.keyColor
    }

    private fun drawDivider(canvas: Canvas, bounds: KeyBounds, x: Float) {
        canvas.drawLine(
            x,
            bounds.top + metrics.dp(DividerInsetDp),
            x,
            bounds.bottom - metrics.dp(DividerInsetDp),
            dividerPaint,
        )
    }

    private companion object {
        const val SuggestionLabelSizeSp = 14f
        const val DividerInsetDp = 9f
        const val DividerAlpha = 0x33000000
    }
}
