package app.funput.funput.theme

import app.funput.funput.theme.validation.ThemeValidator
import kotlin.math.abs
import kotlin.math.pow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The promise re-dyeing makes is that a user cannot break a built-in theme's readability just by
 * choosing a colour they like. These tests are that promise: every built-in theme, re-dyed to every
 * hue on the wheel, still passes the same validator the built-ins pass.
 */
class ThemeRecolorTest {

    private val hues = (0 until 360 step 15).map(Int::toFloat)

    /** Mirrors the floor the theme editor passes; see ThemeDraftState. */
    private val EditorSurfaceTint = 0.07f

    @Test
    fun `every built-in theme survives every hue`() {
        // Both the pure re-dye and the one the editor uses, which lifts near-neutral surfaces so a
        // near-neutral theme still answers the colour it was given.
        listOf(0f, EditorSurfaceTint).forEach { floor ->
            BuiltInKeyboardThemeSource.loadThemes().forEach { descriptor ->
                hues.forEach { hue ->
                    val issues = ThemeValidator.validate(descriptor.theme.tintedTowards(hue, floor))
                    assertTrue(
                        "${descriptor.name} at ${hue.toInt()}° floor $floor reports $issues",
                        issues.isEmpty(),
                    )
                }
            }
        }
    }

    @Test
    fun `a near-neutral surface takes the tint it is given`() {
        val nearGrey = 0xFF303030.toInt()
        val theme = BuiltInKeyboardThemeSource.loadThemes().first().theme.copy(keyColor = nearGrey)

        val tinted = theme.tintedTowards(280f, EditorSurfaceTint).keyColor

        assertTrue("surface did not move from ${'$'}nearGrey", tinted != nearGrey)
        assertTrue("hue landed at ${'$'}{tinted.hueDegrees()}", abs(tinted.hueDegrees() - 280f) < 6f)
    }

    @Test
    fun `re-dyeing holds each colour's luminance`() {
        val theme = BuiltInKeyboardThemeSource.loadThemes().first().theme

        val tinted = theme.tintedTowards(200f)

        // Contrast is a function of relative luminance alone, which is why holding it is what makes
        // the ratios survive. A tolerance of one 255th of a channel is below what an eye resolves.
        listOf(
            theme.keyColor to tinted.keyColor,
            theme.labelColor to tinted.labelColor,
            theme.backgroundEndColor to tinted.backgroundEndColor,
        ).forEach { (before, after) ->
            assertTrue(
                "luminance moved from ${before.luminance()} to ${after.luminance()}",
                abs(before.luminance() - after.luminance()) < 0.004,
            )
        }
    }

    @Test
    fun `greys stay grey`() {
        val grey = 0xFF808080.toInt()
        val theme = BuiltInKeyboardThemeSource.loadThemes().first().theme.copy(keyColor = grey)

        // A hue applied to something with no saturation is still that something.
        assertEquals(grey, theme.tintedTowards(120f).keyColor)
    }

    @Test
    fun `re-dyeing moves the hue it was given`() {
        val theme = BuiltInKeyboardThemeSource.loadThemes().first().theme
        val colourful = theme.copy(keyColor = 0xFFB03030.toInt())

        val hue = colourful.tintedTowards(210f).keyColor.hueDegrees()

        assertTrue("hue landed at $hue", abs(hue - 210f) < 6f)
    }

    @Test
    fun `alpha is left alone`() {
        val translucent = 0x14FFFFFF
        val theme = BuiltInKeyboardThemeSource.loadThemes().first().theme.copy(keyColor = translucent)

        assertEquals(0x14, theme.tintedTowards(300f).keyColor ushr 24)
    }

    private fun Int.luminance(): Double {
        fun linear(channel: Int): Double {
            val value = channel / 255.0
            return if (value <= 0.03928) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear((this shr 16) and 0xFF) +
            0.7152 * linear((this shr 8) and 0xFF) +
            0.0722 * linear(this and 0xFF)
    }
}
