package app.funput.funput.theme

import kotlin.math.abs
import kotlin.math.pow

/**
 * Re-dyes a theme towards one hue while holding every colour's relative luminance.
 *
 * WCAG contrast is a function of relative luminance and nothing else, so a theme recoloured this
 * way keeps the contrast ratios the original had — including the ones `ThemeValidator` checks. That
 * is the whole point: the built-in themes were made to be readable, and a user picking a colour
 * they like should inherit that rather than have to rebuild it out of twenty separate swatches.
 *
 * Themes that were already colourful shift the most, which is what "re-dye this theme" should
 * mean. [minimumSaturation] lifts colours that carry almost none, so that a near-neutral theme
 * still answers the chosen colour instead of appearing to ignore it — the same faint tint
 * Material puts on its own dark surfaces. At zero, greys are left as greys.
 */
fun KeyboardTheme.tintedTowards(
    hueDegrees: Float,
    minimumSaturation: Float = 0f,
): KeyboardTheme = copy(
    backgroundStartColor = backgroundStartColor.tinted(hueDegrees, minimumSaturation),
    backgroundEndColor = backgroundEndColor.tinted(hueDegrees, minimumSaturation),
    keyColor = keyColor.tinted(hueDegrees, minimumSaturation),
    specialKeyColor = specialKeyColor.tinted(hueDegrees, minimumSaturation),
    keyBorderColor = keyBorderColor.tinted(hueDegrees, minimumSaturation),
    keyShadowColor = keyShadowColor.tinted(hueDegrees, minimumSaturation),
    pressedKeyColor = pressedKeyColor.tinted(hueDegrees, minimumSaturation),
    pressedKeyBorderColor = pressedKeyBorderColor.tinted(hueDegrees, minimumSaturation),
    activatedKeyColor = activatedKeyColor.tinted(hueDegrees, minimumSaturation),
    activatedKeyBorderColor = activatedKeyBorderColor.tinted(hueDegrees, minimumSaturation),
    labelColor = labelColor.tinted(hueDegrees, minimumSaturation),
    secondaryLabelColor = secondaryLabelColor.tinted(hueDegrees, minimumSaturation),
    specialLabelColor = specialLabelColor.tinted(hueDegrees, minimumSaturation),
    popupSurfaceColor = popupSurfaceColor.tinted(hueDegrees, minimumSaturation),
    suggestionDividerColor = suggestionDividerColor.tinted(hueDegrees, minimumSaturation),
)

/** The hue of an ARGB colour, in degrees. Undefined for greys, which report zero. */
fun Int.hueDegrees(): Float = toHsl().hue

/**
 * Rewrites this colour's hue, then finds the lightness at which it regains the luminance it had.
 *
 * Holding HSL lightness instead would not do: at the same lightness a yellow is far brighter than
 * a blue, so a re-dye that kept lightness would quietly move every contrast ratio in the theme.
 */
private fun Int.tinted(hueDegrees: Float, minimumSaturation: Float): Int {
    val hsl = toHsl()
    val saturation = maxOf(hsl.saturation, minimumSaturation)
    if (saturation <= GreyThreshold) return this
    val target = relativeLuminance()
    var low = 0.0f
    var high = 1.0f
    var candidate = hsl.lightness
    repeat(SearchSteps) {
        candidate = (low + high) / 2f
        val colour = hslToArgb(hueDegrees, saturation, candidate, alpha)
        if (colour.relativeLuminance() < target) low = candidate else high = candidate
    }
    return hslToArgb(hueDegrees, saturation, candidate, alpha)
}

private val Int.alpha: Int get() = this ushr 24

internal data class Hsl(val hue: Float, val saturation: Float, val lightness: Float)

internal fun Int.toHsl(): Hsl {
    val red = ((this shr 16) and Mask) / MaxChannel
    val green = ((this shr 8) and Mask) / MaxChannel
    val blue = (this and Mask) / MaxChannel
    val max = maxOf(red, green, blue)
    val min = minOf(red, green, blue)
    val lightness = (max + min) / 2f
    val delta = max - min
    if (delta < Epsilon) return Hsl(hue = 0f, saturation = 0f, lightness = lightness)
    val saturation = delta / (1f - abs(2f * lightness - 1f))
    val hue = when (max) {
        red -> 60f * (((green - blue) / delta) % 6f)
        green -> 60f * (((blue - red) / delta) + 2f)
        else -> 60f * (((red - green) / delta) + 4f)
    }
    return Hsl(hue = (hue + 360f) % 360f, saturation = saturation.coerceIn(0f, 1f), lightness = lightness)
}

private fun hslToArgb(hue: Float, saturation: Float, lightness: Float, alpha: Int): Int {
    val chroma = (1f - abs(2f * lightness - 1f)) * saturation
    val section = ((hue % 360f) + 360f) % 360f / 60f
    val second = chroma * (1f - abs((section % 2f) - 1f))
    val (red, green, blue) = when (section.toInt()) {
        0 -> Triple(chroma, second, 0f)
        1 -> Triple(second, chroma, 0f)
        2 -> Triple(0f, chroma, second)
        3 -> Triple(0f, second, chroma)
        4 -> Triple(second, 0f, chroma)
        else -> Triple(chroma, 0f, second)
    }
    val match = lightness - chroma / 2f
    fun channel(value: Float): Int = ((value + match) * MaxChannel).toInt().coerceIn(0, Mask)
    return (alpha shl 24) or (channel(red) shl 16) or (channel(green) shl 8) or channel(blue)
}

private fun Int.relativeLuminance(): Double {
    fun linear(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * linear((this shr 16) and Mask) +
        0.7152 * linear((this shr 8) and Mask) +
        0.0722 * linear(this and Mask)
}

private const val Mask = 0xFF
private const val MaxChannel = 255f
private const val Epsilon = 1e-4f
private const val GreyThreshold = 0.02f
private const val SearchSteps = 24
