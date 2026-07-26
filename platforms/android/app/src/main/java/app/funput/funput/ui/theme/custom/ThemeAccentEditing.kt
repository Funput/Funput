package app.funput.funput.ui.theme.custom

import app.funput.funput.theme.KeyboardTheme
import app.funput.funput.theme.validation.ContrastRatio

/**
 * Applies an accent color to every role that should follow it.
 *
 * Picking an accent used to change only [KeyboardTheme.accentColor], which left the Enter key and
 * the leading suggestion on the previous accent — choosing purple still gave you a gold Enter key.
 *
 * The glyph drawn on the accent key is whichever of the two candidates actually measures better
 * against it. A luminance threshold was the obvious approach and it was wrong for mid-tone colors:
 * a medium gold sits below any reasonable "is this light?" cutoff, so it got a pale glyph that
 * came out at 2.9:1 — under the readability bar the validator enforces.
 */
internal fun KeyboardTheme.withAccent(color: Int): KeyboardTheme = copy(
    accentColor = color,
    accentKeyColor = color,
    accentLabelColor = color.mostReadableGlyph(over = backgroundEndColor),
    suggestionHighlightColor = color,
)

internal fun Int.mostReadableGlyph(over: Int): Int {
    val darkRatio = ContrastRatio.between(DarkGlyph, this, over)
    val lightRatio = ContrastRatio.between(LightGlyph, this, over)
    return if (darkRatio >= lightRatio) DarkGlyph else LightGlyph
}

internal const val DarkGlyph = 0xFF17110A.toInt()
internal const val LightGlyph = 0xFFFFF9EA.toInt()
