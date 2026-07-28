package app.funput.funput.keyboard.popover.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.core.graphics.ColorUtils
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.keyboard.popover.interaction.AlternateSelectionPreview
import app.funput.funput.keyboard.rendering.RenderMetrics
import app.funput.funput.theme.KeyboardTheme

internal class AlternatePaletteRenderer(private val metrics: RenderMetrics) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val rect = RectF()
    private val fontMetrics = Paint.FontMetrics()

    fun draw(
        canvas: Canvas,
        preview: AlternateSelectionPreview,
        theme: KeyboardTheme,
        shiftState: ShiftState,
    ) {
        val bounds = preview.layout.bounds
        val radius = metrics.dp(12f)
        rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        shadowPaint.color = theme.popupShadowColor
        rect.offset(0f, metrics.dp(2f))
        canvas.drawRoundRect(rect, radius, radius, shadowPaint)
        rect.offset(0f, -metrics.dp(2f))
        fillPaint.color = theme.popupSurfaceColor
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
        drawBorder(canvas, theme, radius)
        preview.layout.itemBounds.forEachIndexed { index, item ->
            if (index == preview.selectedIndex) drawSelection(canvas, item, theme, radius)
            drawLabel(canvas, item.centerX, item.centerY, preview.key.alternates[index].textFor(shiftState), theme)
        }
    }

    private fun drawSelection(canvas: Canvas, bounds: app.funput.funput.keyboard.layout.KeyBounds, theme: KeyboardTheme, radius: Float) {
        fillPaint.color = ColorUtils.setAlphaComponent(theme.accentColor, SelectionAlpha)
        rect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        canvas.drawRoundRect(rect, radius, radius, fillPaint)
    }

    private fun drawLabel(canvas: Canvas, x: Float, y: Float, text: String, theme: KeyboardTheme) {
        labelPaint.color = theme.labelColor
        labelPaint.textSize = metrics.sp(22f)
        labelPaint.getFontMetrics(fontMetrics)
        canvas.drawText(text, x, y - (fontMetrics.ascent + fontMetrics.descent) / 2f, labelPaint)
    }

    private fun drawBorder(canvas: Canvas, theme: KeyboardTheme, radius: Float) {
        if (theme.keyBorderWidthDp <= 0f) return
        borderPaint.color = theme.keyBorderColor
        borderPaint.strokeWidth = metrics.dp(theme.keyBorderWidthDp)
        canvas.drawRoundRect(rect, radius, radius, borderPaint)
    }

    private companion object {
        const val SelectionAlpha = 71
    }
}
