package app.funput.funput.keyboard.rendering

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import app.funput.funput.theme.KeyboardKeySurfaceStyle
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/** Cached lighting treatment used only by glass key surfaces. */
internal class GlassKeySurfacePainter(private val metrics: RenderMetrics) {
    private val haloPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val haloRect = RectF()
    private var normalBorderShader: Shader? = null
    private var pressedBorderShader: Shader? = null
    private var activatedBorderShader: Shader? = null

    fun updateTheme(theme: KeyboardTheme, width: Int, height: Int) {
        haloPaint.strokeWidth = metrics.dp(HaloStrokeDp)
        val enabled = theme.keySurfaceStyle == KeyboardKeySurfaceStyle.GLASS
        normalBorderShader = borderShader(theme.keyBorderColor, width, height, enabled)
        pressedBorderShader = borderShader(theme.pressedKeyBorderColor, width, height, enabled)
        activatedBorderShader = borderShader(theme.activatedKeyBorderColor, width, height, enabled)
    }

    fun drawPressedHalo(
        canvas: Canvas,
        drawingRect: RectF,
        radius: Float,
        theme: KeyboardTheme,
    ) {
        val color = theme.pressedKeyBorderColor.scaleAlpha(HaloAlpha)
        if (!color.isVisible) return
        val expansion = metrics.dp(HaloExpansionDp)
        haloRect.set(drawingRect)
        haloRect.inset(-expansion, -expansion)
        haloPaint.color = color
        canvas.drawRoundRect(haloRect, radius + expansion, radius + expansion, haloPaint)
    }

    fun borderShader(isPressed: Boolean, isActivated: Boolean): Shader? = when {
        isPressed -> pressedBorderShader
        isActivated -> activatedBorderShader
        else -> normalBorderShader
    }

    private fun borderShader(color: Int, width: Int, height: Int, enabled: Boolean): Shader? {
        if (!enabled || !color.isVisible || width <= 0 || height <= 0) return null
        return LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            color,
            color.scaleAlpha(RimEndAlpha),
            Shader.TileMode.CLAMP,
        )
    }

    private val Int.isVisible: Boolean get() = this ushr AlphaShift != 0

    private fun Int.scaleAlpha(factor: Float): Int {
        val alpha = ((this ushr AlphaShift) * factor).roundToInt().coerceIn(0, MaxAlpha)
        return (this and RgbMask) or (alpha shl AlphaShift)
    }

    private companion object {
        const val AlphaShift = 24
        const val MaxAlpha = 255
        const val RgbMask = 0x00FFFFFF
        const val RimEndAlpha = 0.18f
        const val HaloAlpha = 0.22f
        const val HaloExpansionDp = 2f
        const val HaloStrokeDp = 2f
    }
}
