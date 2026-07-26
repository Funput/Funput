package app.funput.funput.theme.validation

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * WCAG 2.1 relative-luminance contrast between two ARGB colors.
 *
 * Both colors are composited onto [background] first. Skipping that step is a real source of
 * wrong answers for this keyboard: the dark theme paints its keys as white at 8% alpha, and
 * judging that as opaque white against a white label would report a comfortable ratio for text
 * that is in fact invisible.
 */
object ContrastRatio {
    fun between(foreground: Int, surface: Int, background: Int): Double {
        val resolvedSurface = surface.composited(over = background)
        val resolvedForeground = foreground.composited(over = resolvedSurface)
        val lighter = max(resolvedForeground.luminance(), resolvedSurface.luminance())
        val darker = min(resolvedForeground.luminance(), resolvedSurface.luminance())
        return (lighter + Offset) / (darker + Offset)
    }

    /** Flattens a translucent color onto an opaque one. */
    private fun Int.composited(over: Int): Int {
        val alpha = (this ushr 24) / MaxChannel
        if (alpha >= 1.0) return this
        fun blend(shift: Int): Int {
            val top = (this shr shift) and ChannelMask
            val bottom = (over shr shift) and ChannelMask
            return (top * alpha + bottom * (1.0 - alpha)).toInt().coerceIn(0, ChannelMask)
        }
        return (0xFF shl 24) or (blend(16) shl 16) or (blend(8) shl 8) or blend(0)
    }

    private fun Int.luminance(): Double {
        val red = linear(((this shr 16) and ChannelMask) / MaxChannel)
        val green = linear(((this shr 8) and ChannelMask) / MaxChannel)
        val blue = linear((this and ChannelMask) / MaxChannel)
        return RedWeight * red + GreenWeight * green + BlueWeight * blue
    }

    private fun linear(channel: Double): Double =
        if (channel <= LinearThreshold) {
            channel / LinearDivisor
        } else {
            ((channel + GammaOffset) / GammaDivisor).pow(GammaExponent)
        }

    private const val ChannelMask = 0xFF
    private const val MaxChannel = 255.0
    private const val Offset = 0.05
    private const val RedWeight = 0.2126
    private const val GreenWeight = 0.7152
    private const val BlueWeight = 0.0722
    private const val LinearThreshold = 0.03928
    private const val LinearDivisor = 12.92
    private const val GammaOffset = 0.055
    private const val GammaDivisor = 1.055
    private const val GammaExponent = 2.4
}
