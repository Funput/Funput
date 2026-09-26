package app.funput.build.tokens

/** An sRGB colour with straight (not premultiplied) alpha, each channel 0..255. */
data class RgbaColor(val red: Int, val green: Int, val blue: Int, val alpha: Int = 255) {
    init {
        require(listOf(red, green, blue, alpha).all { it in 0..255 }) { "channel out of range: $this" }
    }

    /** Whether nothing shows through: the colour can be a background on its own. */
    val isOpaque: Boolean
        get() = alpha == 255

    companion object {
        private val Hex = Regex("^#([0-9A-Fa-f]{6})([0-9A-Fa-f]{2})?$")

        /** Parses `#RRGGBB` or `#RRGGBBAA` (alpha last, CSS order), or returns null. */
        fun parse(text: String): RgbaColor? {
            val match = Hex.matchEntire(text) ?: return null
            val rgb = match.groupValues[1].toInt(16)
            val alpha = match.groupValues[2].takeIf { it.isNotEmpty() }?.toInt(16) ?: 255
            return RgbaColor(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF, alpha)
        }
    }
}

/** One colour role, resolved separately for the light and the dark appearance. */
data class ColorPair(val light: RgbaColor, val dark: RgbaColor)

/** A text style: point/sp size, line height in the same unit, and a CSS-style weight. */
data class TextStyleToken(val size: Double, val lineHeight: Double, val weight: Int)

/**
 * An animation, kept in the iOS form it was measured from. Platforms translate it: SwiftUI takes
 * `spring(duration:bounce:)` directly, Compose derives a damping ratio and stiffness.
 */
data class MotionToken(val curve: MotionCurve, val durationMs: Int, val bounce: Double?)

/** The easing families the app uses; `SPRING` is the only one that reads `bounce`. */
enum class MotionCurve(val jsonName: String) {
    SPRING("spring"),
    EASE_OUT("easeOut"),
    EASE_IN_OUT("easeInOut"),
    LINEAR("linear"),
    ;

    companion object {
        /** The curve spelled [name] in the token file, or null when it is not one of ours. */
        fun fromJson(name: String): MotionCurve? = entries.firstOrNull { it.jsonName == name }
    }
}

/**
 * The shared app design tokens (`design/tokens/app.tokens.json`) after parsing.
 *
 * Numeric groups (`radius`, `spacing`, `layout`, `opacity`) are plain name→value maps so a new
 * token needs no code change; the rules decide which of them must exist and what range they obey.
 */
data class DesignTokens(
    val schemaVersion: Int,
    val colors: Map<String, ColorPair>,
    val numbers: Map<String, Map<String, Double>>,
    val typography: Map<String, TextStyleToken>,
    val motion: Map<String, MotionToken>,
) {
    companion object {
        /** The only schema this build understands; bump it together with the parser. */
        const val SUPPORTED_SCHEMA_VERSION: Int = 1

        /** The groups parsed as name→number maps. */
        val NumberGroups: List<String> = listOf("radius", "spacing", "layout", "opacity")
    }
}
