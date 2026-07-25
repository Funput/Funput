package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.roundToInt

class CustomThemeOverridesTest {
    @Test
    fun keyOpacityScalesWhatTheBaseAlreadyDrawsInsteadOfAssigningAFlatAlpha() {
        val base = KeyboardThemes.Ink

        val theme = CustomThemeOverrides(keyBackgroundOpacity = KeyOpacity).applyTo(base)

        assertEquals(
            ((base.keyColor ushr AlphaShift) * KeyOpacity).roundToInt(),
            theme.keyColor ushr AlphaShift,
        )
    }

    @Test
    fun keyOpacityKeepsATransparentKeySurfaceTransparent() {
        val plateless = KeyboardThemes.Ink.copy(keyColor = 0x00000000, specialKeyColor = 0x00000000)

        val theme = CustomThemeOverrides(keyBackgroundOpacity = KeyOpacity).applyTo(plateless)

        assertEquals(0, theme.keyColor ushr AlphaShift)
        assertEquals(0, theme.specialKeyColor ushr AlphaShift)
    }

    @Test
    fun keyOpacityScalesAnOpaqueBaseProportionally() {
        val theme = CustomThemeOverrides(keyBackgroundOpacity = KeyOpacity).applyTo(KeyboardThemes.Paper)

        assertEquals(OpaqueScaledAlpha, theme.keyColor ushr AlphaShift)
        assertEquals(OpaqueScaledAlpha, theme.specialKeyColor ushr AlphaShift)
    }

    @Test
    fun accentOverrideReplacesOnlyTheAccentColor() {
        val theme = CustomThemeOverrides(accentColor = OceanAccent).applyTo(KeyboardThemes.Ink)

        assertEquals(OceanAccent, theme.accentColor)
        assertEquals(KeyboardThemes.Ink.labelColor, theme.labelColor)
    }

    @Test
    fun absentOverridesLeaveTheBaseUntouched() {
        assertEquals(KeyboardThemes.Ink, CustomThemeOverrides().applyTo(KeyboardThemes.Ink))
    }

    @Test
    fun outOfRangeKeyOpacityIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CustomThemeOverrides(keyBackgroundOpacity = 1.5f)
        }
    }

    private companion object {
        const val OceanAccent = 0xFF0099CC.toInt()
        const val KeyOpacity = 0.62f
        const val OpaqueScaledAlpha = 158
        const val AlphaShift = 24
    }
}
