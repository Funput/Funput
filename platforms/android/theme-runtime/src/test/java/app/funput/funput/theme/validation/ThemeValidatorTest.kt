package app.funput.funput.theme.validation

import app.funput.funput.theme.KeyboardThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeValidatorTest {
    @Test
    fun bundledThemesAreReadable() {
        assertEquals(emptyList<ThemeIssue>(), ThemeValidator.validate(KeyboardThemes.Ink))
        assertEquals(emptyList<ThemeIssue>(), ThemeValidator.validate(KeyboardThemes.Paper))
        assertEquals(emptyList<ThemeIssue>(), ThemeValidator.validate(KeyboardThemes.GlassDark))
        assertEquals(emptyList<ThemeIssue>(), ThemeValidator.validate(KeyboardThemes.GlassLight))
        assertEquals(emptyList<ThemeIssue>(), ThemeValidator.validate(KeyboardThemes.Slate))
    }

    @Test
    fun aLabelMatchingItsKeyIsReported() {
        val theme = KeyboardThemes.Paper.copy(labelColor = KeyboardThemes.Paper.keyColor)

        val pairs = ThemeValidator.validate(theme).map(ThemeIssue::pair)

        assertTrue(ThemeContrastPair.LabelOnKey in pairs)
    }

    @Test
    fun aTranslucentKeyIsJudgedAgainstTheBackgroundItSitsOn() {
        // Ink paints keys as white at 8% alpha over near-black. Treating that fill as opaque
        // white would rate a white label as unreadable; composited, it is fine.
        val opaqueReading = ContrastRatio.between(
            foreground = KeyboardThemes.Ink.labelColor,
            surface = 0xFFFFFFFF.toInt(),
            background = KeyboardThemes.Ink.backgroundEndColor,
        )
        val compositedReading = ContrastRatio.between(
            foreground = KeyboardThemes.Ink.labelColor,
            surface = KeyboardThemes.Ink.keyColor,
            background = KeyboardThemes.Ink.backgroundEndColor,
        )

        assertTrue(opaqueReading < ThemeValidator.MinimumContrast)
        assertTrue(compositedReading > ThemeValidator.MinimumContrast)
    }

    @Test
    fun theKeyOpacityControlIsAccountedForWhenJudgingAKey() {
        // Fading Paper's white keys almost away leaves dark ink on a dark ground.
        val theme = KeyboardThemes.Paper.copy(
            backgroundEndColor = 0xFF101010.toInt(),
            keyOpacity = 0.02f,
        )

        val pairs = ThemeValidator.validate(theme).map(ThemeIssue::pair)

        assertTrue(ThemeContrastPair.LabelOnKey in pairs)
    }

    @Test
    fun identicalColorsGiveTheLowestPossibleRatio() {
        val ratio = ContrastRatio.between(
            foreground = 0xFF808080.toInt(),
            surface = 0xFF808080.toInt(),
            background = 0xFF000000.toInt(),
        )

        assertEquals(1.0, ratio, 0.0001)
    }

    @Test
    fun blackOnWhiteGivesTheMaximumRatio() {
        val ratio = ContrastRatio.between(
            foreground = 0xFF000000.toInt(),
            surface = 0xFFFFFFFF.toInt(),
            background = 0xFFFFFFFF.toInt(),
        )

        assertEquals(21.0, ratio, 0.01)
    }
}
