package app.funput.funput.theme.validation

import app.funput.funput.theme.KeyboardTheme

/** A readability problem found in a theme. Advisory only — nothing here blocks saving. */
data class ThemeIssue(val pair: ThemeContrastPair, val ratio: Double)

/** A foreground/background pairing the keyboard actually draws. */
enum class ThemeContrastPair {
    LabelOnKey,
    SpecialLabelOnSpecialKey,
    SecondaryLabelOnKey,
    AccentLabelOnAccentKey,
    SuggestionHighlightOnBackground,
}

/**
 * Checks the color pairings a reader has to resolve while typing.
 *
 * The pairs are chosen from what this renderer draws rather than copied from another platform:
 * modifier keys use their own label color here, and the Enter glyph sits on the accent key. Every
 * surface is judged against the keyboard background so translucent keys are measured as seen.
 */
object ThemeValidator {
    /** WCAG 2.1 AA for large text. Key labels are large and bold enough to use this bar. */
    const val MinimumContrast = 3.0

    fun validate(theme: KeyboardTheme): List<ThemeIssue> {
        val background = theme.backgroundEndColor.opaque()
        return ThemeContrastPair.entries.mapNotNull { pair ->
            val ratio = ContrastRatio.between(
                foreground = pair.foreground(theme),
                surface = pair.surface(theme),
                background = background,
            )
            ThemeIssue(pair, ratio).takeIf { ratio < MinimumContrast }
        }
    }

    private fun ThemeContrastPair.foreground(theme: KeyboardTheme): Int = when (this) {
        ThemeContrastPair.LabelOnKey -> theme.labelColor
        ThemeContrastPair.SpecialLabelOnSpecialKey -> theme.specialLabelColor
        ThemeContrastPair.SecondaryLabelOnKey -> theme.secondaryLabelColor
        ThemeContrastPair.AccentLabelOnAccentKey -> theme.accentLabelColor
        ThemeContrastPair.SuggestionHighlightOnBackground -> theme.suggestionHighlightColor
    }

    private fun ThemeContrastPair.surface(theme: KeyboardTheme): Int = when (this) {
        ThemeContrastPair.LabelOnKey,
        ThemeContrastPair.SecondaryLabelOnKey -> theme.keyColor.scaled(theme.keyOpacity)
        ThemeContrastPair.SpecialLabelOnSpecialKey ->
            theme.specialKeyColor.scaled(theme.specialKeyOpacity)
        ThemeContrastPair.AccentLabelOnAccentKey -> theme.accentKeyColor
        // The suggestion bar has no surface of its own; it sits straight on the background.
        ThemeContrastPair.SuggestionHighlightOnBackground -> Transparent
    }

    /** Mirrors the opacity the painter applies, so the check matches what is drawn. */
    private fun Int.scaled(opacity: Float): Int {
        if (opacity >= 1f) return this
        val alpha = ((this ushr 24) * opacity).toInt().coerceIn(0, MaxAlpha)
        return (this and RgbMask) or (alpha shl AlphaShift)
    }

    private fun Int.opaque(): Int = this or (MaxAlpha shl AlphaShift)

    private const val Transparent = 0x00000000
    private const val MaxAlpha = 0xFF
    private const val AlphaShift = 24
    private const val RgbMask = 0x00FFFFFF
}
