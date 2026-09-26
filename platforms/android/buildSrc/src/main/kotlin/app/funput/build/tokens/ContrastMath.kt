package app.funput.build.tokens

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * WCAG 2.x contrast for build-time checks.
 *
 * `theme-runtime` has its own `ContrastRatio` for keyboard themes, but buildSrc is compiled before
 * any project module and cannot depend on one, so the arithmetic is repeated here. It is the WCAG
 * formula, not a Funput choice, so the two copies cannot meaningfully drift.
 */
object ContrastMath {
    /** The contrast ratio (1..21) of [foreground] drawn over the opaque [background]. */
    fun ratio(foreground: RgbaColor, background: RgbaColor): Double {
        require(background.isOpaque) { "contrast needs an opaque background, got $background" }
        val a = luminance(composite(foreground, background))
        val b = luminance(background)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }

    /** [color] blended over the opaque [background]: what a translucent colour really shows. */
    fun composite(color: RgbaColor, background: RgbaColor): RgbaColor {
        val alpha = color.alpha / 255.0
        fun blend(top: Int, bottom: Int) = (top * alpha + bottom * (1 - alpha)).roundToInt()
        return RgbaColor(
            red = blend(color.red, background.red),
            green = blend(color.green, background.green),
            blue = blend(color.blue, background.blue),
        )
    }

    /** WCAG relative luminance (0 black .. 1 white) of an opaque colour. */
    fun luminance(color: RgbaColor): Double =
        0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)

    private fun linear(channel: Int): Double {
        val value = channel / 255.0
        return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
    }
}
