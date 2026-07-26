package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import app.funput.funput.keyboard.layout.ResolvedKey
import app.funput.funput.keyboard.model.KeyRole
import app.funput.funput.theme.KeyboardKeySurfaceStyle
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

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
    private val glassPainter = GlassKeySurfacePainter(metrics)

    fun updateTheme(theme: KeyboardTheme, width: Int, height: Int) {
        shadowPaint.color = theme.keyShadowColor
        borderPaint.strokeWidth = metrics.dp(theme.keyBorderWidthDp)
        glassPainter.updateTheme(theme, width, height)
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
        setDrawingRect(key, theme, if (isPressed) theme.pressedKeyScale else 1f)
        drawShadow(canvas, theme, radius, isPressed)
        if (theme.keySurfaceStyle == KeyboardKeySurfaceStyle.GLASS && isPressed) {
            glassPainter.drawPressedHalo(canvas, drawingRect, radius, theme)
        }
        if (fillColor.isVisible) {
            fillPaint.color = fillColor
            canvas.drawRoundRect(drawingRect, radius, radius, fillPaint)
        }
        if (hasBorder) {
            borderPaint.shader = if (theme.keySurfaceStyle == KeyboardKeySurfaceStyle.GLASS) {
                glassPainter.borderShader(isPressed, isActivated)
            } else {
                null
            }
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

    /**
     * Shapes the painted rectangle only. [ResolvedKey.bounds] is what hit testing reads, so it is
     * never touched here — a theme can shrink how a key looks but not where it can be pressed.
     */
    private fun setDrawingRect(key: ResolvedKey, theme: KeyboardTheme, scale: Float) {
        val bounds = key.bounds
        drawingRect.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
        val inset = metrics.dp(theme.keycapInsetDp)
        if (inset > 0f) {
            // Never inset so far that the key vanishes, however the theme was authored.
            val limit = minOf(bounds.width, bounds.height) / MaxInsetDivisor
            drawingRect.inset(minOf(inset, limit), minOf(inset, limit))
        }
        if (scale == 1f) return
        drawingRect.inset(
            -drawingRect.width() * (scale - 1f) / 2f,
            -drawingRect.height() * (scale - 1f) / 2f,
        )
    }

    /**
     * The surface color, dimmed by the theme's opacity control.
     *
     * The control scales the authored alpha rather than replacing it, so a theme whose keys are a
     * faint wash stays a faint wash, and a plateless theme stays plateless.
     */
    private fun fillColor(
        key: ResolvedKey,
        theme: KeyboardTheme,
        isPressed: Boolean,
        isActivated: Boolean,
    ): Int {
        val opacity =
            if (key.spec.role.isSpecial) theme.specialKeyOpacity else theme.keyOpacity
        return baseFillColor(key, theme, isPressed, isActivated).scaleAlpha(opacity)
    }

    private fun baseFillColor(
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

    private fun Int.scaleAlpha(factor: Float): Int {
        if (factor >= 1f) return this
        val alpha = ((this ushr 24) * factor).roundToInt().coerceIn(0, MaxAlpha)
        return (this and RgbMask) or (alpha shl 24)
    }

    private companion object {
        const val MaxAlpha = 255
        const val RgbMask = 0x00FFFFFF
        const val MaxInsetDivisor = 4f
    }
}
