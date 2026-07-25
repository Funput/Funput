package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.theme.KeyboardTheme

/**
 * Paints the plate behind a key: shadow, fill, and border.
 *
 * Every pass is skipped when its color is fully transparent, so a theme that sets no key color
 * costs nothing to draw rather than painting invisible rectangles under each glyph.
 */
internal class KeySurfacePainter(private val metrics: RenderMetrics) {
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val drawingRect = RectF()

    fun updateTheme(theme: KeyboardTheme) {
        shadowPaint.color = theme.keyShadowColor
        borderPaint.strokeWidth = metrics.dp(theme.keyBorderWidthDp)
    }

    fun draw(
        canvas: Canvas,
        key: ResolvedKey,
        theme: KeyboardTheme,
        isPressed: Boolean,
        isActivated: Boolean,
    ) {
        val fillColor = fillColor(key, theme, isPressed, isActivated)
        val borderColor = borderColor(theme, isPressed, isActivated)
        val hasBorder = theme.keyBorderWidthDp > 0f && borderColor.isVisible
        if (!fillColor.isVisible && !hasBorder) return

        val radius = metrics.dp(theme.keyCornerRadiusDp)
        setDrawingRect(key, if (isPressed) theme.pressedKeyScale else 1f)
        drawShadow(canvas, theme, radius, isPressed)
        if (fillColor.isVisible) {
            fillPaint.color = fillColor
            canvas.drawRoundRect(drawingRect, radius, radius, fillPaint)
        }
        if (hasBorder) {
            borderPaint.color = borderColor
            canvas.drawRoundRect(drawingRect, radius, radius, borderPaint)
        }
    }

    private fun drawShadow(canvas: Canvas, theme: KeyboardTheme, radius: Float, isPressed: Boolean) {
        val offset = metrics.dp(
            if (isPressed) theme.pressedKeyShadowOffsetDp else theme.keyShadowOffsetDp,
        )
        if (offset <= 0f || !theme.keyShadowColor.isVisible) return
        drawingRect.offset(0f, offset)
        canvas.drawRoundRect(drawingRect, radius, radius, shadowPaint)
        drawingRect.offset(0f, -offset)
    }

    private fun setDrawingRect(key: ResolvedKey, scale: Float) {
        val bounds = key.bounds
        drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        if (scale == 1f) return
        drawingRect.inset(
            -bounds.width * (scale - 1f) / 2f,
            -bounds.height * (scale - 1f) / 2f,
        )
    }

    private fun fillColor(
        key: ResolvedKey,
        theme: KeyboardTheme,
        isPressed: Boolean,
        isActivated: Boolean,
    ): Int = when {
        isPressed -> theme.pressedKeyColor
        isActivated -> theme.activatedKeyColor
        key.spec.role == KeyRole.ENTER -> theme.accentKeyColor
        key.spec.role.isSpecial -> theme.specialKeyColor
        else -> theme.keyColor
    }

    private fun borderColor(theme: KeyboardTheme, isPressed: Boolean, isActivated: Boolean): Int =
        when {
            isPressed -> theme.pressedKeyBorderColor
            isActivated -> theme.activatedKeyBorderColor
            else -> theme.keyBorderColor
        }

    private val Int.isVisible: Boolean get() = this ushr 24 != 0
}
