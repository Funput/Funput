package app.funput.funput.keyboard.popover.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.ShiftState
import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.keyboard.rendering.RenderMetrics

/** Draws the enlarged key preview above a pressed printable key. */
internal class KeyPopupRenderer(private val metrics: RenderMetrics) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    }
    private val drawingRect = RectF()
    private val fontMetrics = Paint.FontMetrics()

    fun draw(
        canvas: Canvas,
        key: ResolvedKey,
        surfaceWidth: Float,
        theme: KeyboardTheme,
        shiftState: ShiftState,
    ) {
        val bounds = KeyPopupLayout.bounds(
            key = key,
            surfaceWidth = surfaceWidth,
            popupWidth = maxOf(key.bounds.width * WidthScale, metrics.dp(MinWidthDp)),
            popupHeight = maxOf(key.bounds.height * HeightScale, metrics.dp(MinHeightDp)),
            edgeInset = metrics.dp(EdgeInsetDp),
            anchorOverlap = metrics.dp(AnchorOverlapDp),
        )
        val radius = metrics.dp(theme.keyCornerRadiusDp)
        drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        shadowPaint.color = theme.popupShadowColor
        drawingRect.offset(0f, metrics.dp(ShadowOffsetDp))
        canvas.drawRoundRect(drawingRect, radius, radius, shadowPaint)
        drawingRect.offset(0f, -metrics.dp(ShadowOffsetDp))
        fillPaint.color = theme.popupSurfaceColor
        canvas.drawRoundRect(drawingRect, radius, radius, fillPaint)
        drawBorder(canvas, theme, radius)
        drawLabel(canvas, key, bounds.centerX, bounds.centerY, theme, shiftState)
    }

    private fun drawBorder(canvas: Canvas, theme: KeyboardTheme, radius: Float) {
        if (theme.keyBorderWidthDp <= 0f) return
        borderPaint.color = theme.keyBorderColor
        borderPaint.strokeWidth = metrics.dp(theme.keyBorderWidthDp)
        canvas.drawRoundRect(drawingRect, radius, radius, borderPaint)
    }

    private fun drawLabel(
        canvas: Canvas,
        key: ResolvedKey,
        centerX: Float,
        centerY: Float,
        theme: KeyboardTheme,
        shiftState: ShiftState,
    ) {
        labelPaint.color = theme.labelColor
        labelPaint.textSize = metrics.sp(LabelSizeSp)
        labelPaint.getFontMetrics(fontMetrics)
        val baseline = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(KeyPopupLayout.label(key, shiftState), centerX, baseline, labelPaint)
    }

    private companion object {
        const val WidthScale = 1.4f
        const val HeightScale = 1.45f
        const val MinWidthDp = 56f
        const val MinHeightDp = 58f
        const val EdgeInsetDp = 4f
        const val AnchorOverlapDp = 6f
        const val ShadowOffsetDp = 2f
        const val LabelSizeSp = 30f
    }
}
