package app.funput.funput.keyboard.rendering

import android.graphics.Paint
import android.graphics.Typeface
import app.funput.funput.theme.KeyboardLabelWeight
import app.funput.funput.theme.KeyboardTheme

/**
 * The size and weight a theme asks key labels to be drawn at.
 *
 * Weights resolve to the platform's named sans-serif families rather than a numeric weight, since
 * `Typeface.create(Typeface, Int, Boolean)` needs API 28 and minSdk is 26. These four families
 * exist on every supported release, so a theme looks the same on all of them.
 */
internal object KeyLabelTypography {
    private val typefaces: Map<KeyboardLabelWeight, Typeface> by lazy {
        KeyboardLabelWeight.entries.associateWith { weight ->
            val font = fontFor(weight)
            Typeface.create(font.family, font.style)
        }
    }

    fun fontFor(weight: KeyboardLabelWeight): LabelFont = when (weight) {
        KeyboardLabelWeight.LIGHT -> LabelFont("sans-serif-light", Typeface.NORMAL)
        KeyboardLabelWeight.REGULAR -> LabelFont("sans-serif", Typeface.NORMAL)
        KeyboardLabelWeight.MEDIUM -> LabelFont("sans-serif-medium", Typeface.NORMAL)
        KeyboardLabelWeight.BOLD -> LabelFont("sans-serif", Typeface.BOLD)
    }

    /** Letters and other character labels. */
    fun characterTypeface(theme: KeyboardTheme): Typeface = typefaces.getValue(theme.labelWeight)

    /** Modifier labels, Space and Enter: one step heavier so they still read as another kind of key. */
    fun specialTypeface(theme: KeyboardTheme): Typeface = typefaces.getValue(theme.labelWeight.heavier())

    /**
     * How much to shrink text [textWidth] wide so it fits in [maxWidth]; 1 when it already fits.
     *
     * A larger label size is chosen against letter keys, and a word on a narrow modifier key
     * ("ABC") would otherwise spill past its plate at the top of the range.
     */
    fun fitFactor(textWidth: Float, maxWidth: Float): Float =
        if (textWidth > maxWidth && maxWidth > 0f) maxWidth / textWidth else 1f
}

/** A platform font family and style, kept apart from [Typeface] so the choice can be unit tested. */
internal data class LabelFont(val family: String, val style: Int)

/** A label size in sp, scaled by the theme on top of the keyboard's own sizing. */
internal fun RenderMetrics.labelSp(sizeSp: Float, theme: KeyboardTheme): Float = sp(sizeSp) * theme.labelScale

/** Shrinks the text size, never grows it, until [text] fits in [maxWidth]. */
internal fun Paint.shrinkToFit(text: String, maxWidth: Float) {
    textSize *= KeyLabelTypography.fitFactor(measureText(text), maxWidth)
}
