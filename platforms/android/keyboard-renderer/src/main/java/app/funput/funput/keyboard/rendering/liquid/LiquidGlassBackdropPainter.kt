package app.funput.funput.keyboard.rendering.liquid

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import app.funput.funput.theme.KeyboardKeySurfaceStyle
import app.funput.funput.theme.KeyboardTheme
import kotlin.math.roundToInt

/**
 * The out-of-focus field the keys are glass over.
 *
 * On iOS the material samples the app behind the keyboard, so every key picks up whatever
 * happens to sit under it. An IME window on Android cannot read that, and a keyboard drawn over
 * flat color has nothing to refract — clear keys just look grey. This stands in for the missing
 * sample: wide, heavily overlapping blooms of desaturated color, laid over the theme's own
 * gradient, which the translucent plates then pick up differently from row to row.
 *
 * The blooms are deliberately larger than the surface and their falloff runs to nothing, so what
 * shows is the middle of each one. No edge of a blob is ever visible, which is what keeps it
 * reading as depth behind glass rather than as a pattern painted on the keyboard.
 */
internal class LiquidGlassBackdropPainter {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var blooms: Array<Shader> = emptyArray()

    fun updateTheme(theme: KeyboardTheme, width: Int, height: Int) {
        blooms = if (theme.keySurfaceStyle != KeyboardKeySurfaceStyle.LIQUID_GLASS ||
            width <= 0 || height <= 0
        ) {
            emptyArray()
        } else {
            Array(Field.size) { index -> Field[index].shader(width.toFloat(), height.toFloat()) }
        }
    }

    fun draw(canvas: Canvas, width: Int, height: Int) {
        if (blooms.isEmpty()) return
        val right = width.toFloat()
        val bottom = height.toFloat()
        for (index in blooms.indices) {
            paint.shader = blooms[index]
            canvas.drawRect(0f, 0f, right, bottom, paint)
        }
        paint.shader = null
    }

    /** One bloom, placed and sized in fractions of the surface so it survives any keyboard height. */
    private class Bloom(
        private val centerX: Float,
        private val centerY: Float,
        private val radius: Float,
        private val color: Int,
    ) {
        /**
         * Four stops rather than two: a straight ramp to transparent leaves a visible disc, while
         * easing out over the last third reads as something genuinely out of focus.
         */
        fun shader(width: Float, height: Float): Shader = RadialGradient(
            centerX * width,
            centerY * height,
            radius * width,
            intArrayOf(color, color.fade(NearAlpha), color.fade(FarAlpha), color.fade(0f)),
            Falloff,
            Shader.TileMode.CLAMP,
        )

        private fun Int.fade(factor: Float): Int {
            val alpha = ((this ushr AlphaShift) * factor).roundToInt().coerceIn(0, MaxAlpha)
            return (this and RgbMask) or (alpha shl AlphaShift)
        }
    }

    private companion object {
        const val AlphaShift = 24
        const val MaxAlpha = 255
        const val RgbMask = 0x00FFFFFF
        const val NearAlpha = 0.58f
        const val FarAlpha = 0.2f
        val Falloff = floatArrayOf(0f, 0.36f, 0.68f, 1f)

        /**
         * Muted, and never more than one bloom of a given family. Saturated color would turn the
         * keyboard into stained glass; these read as a room's worth of light behind the panes.
         */
        val Field = arrayOf(
            Bloom(0.24f, 0.78f, 0.95f, 0x6631664C),
            Bloom(0.48f, -0.1f, 0.88f, 0x59564474),
            Bloom(1.04f, 0.38f, 0.82f, 0x4D3F5167),
            Bloom(0.76f, 1.08f, 0.72f, 0x40566070),
        )
    }
}
