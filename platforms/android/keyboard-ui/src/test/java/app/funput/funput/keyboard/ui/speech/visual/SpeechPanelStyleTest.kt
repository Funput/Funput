package app.funput.funput.keyboard.ui.speech.visual

import app.funput.funput.theme.KeyboardThemes
import app.funput.funput.theme.LocalKeyboardThemeCatalog
import app.funput.funput.theme.validation.ContrastRatio
import org.junit.Assert.*
import org.junit.Test

class SpeechPanelStyleTest {
    @Test fun allPresetButtonsHaveReadableContrast() {
        LocalKeyboardThemeCatalog.themes.forEach { descriptor ->
            val style = SpeechPanelStyle.from(descriptor.theme)
            assertReadable(style)
        }
    }

    @Test fun hostileCustomThemeStillHasReadableControls() {
        val custom = KeyboardThemes.Paper.copy(accentKeyColor = 0x00FFFFFF,
            accentLabelColor = 0xFFFDFDFD.toInt(), specialKeyColor = 0x00000000,
            specialLabelColor = 0xFFE8E2D6.toInt(), labelColor = 0xFFFFFFFF.toInt(),
            secondaryLabelColor = 0xFFFFFFFF.toInt(), keyCornerRadiusDp = 40f)
        assertReadable(SpeechPanelStyle.from(custom))
    }

    @Test fun customAccentTokensDetermineThePrimaryAction() {
        val theme = KeyboardThemes.GlassDark.copy(accentKeyColor = 0xFF386F5B.toInt(),
            accentLabelColor = 0xFFFFFFFF.toInt())
        val style = SpeechPanelStyle.from(theme)
        assertEquals(theme.accentKeyColor, style.primarySurface)
        assertEquals(theme.accentLabelColor, style.primaryLabel)
        assertNotEquals(SpeechPanelStyle.from(KeyboardThemes.GlassDark).primarySurface, style.primarySurface)
    }

    @Test fun orbKeepsTheCustomThemeAccentInsteadOfUsingASeparateVoicePalette() {
        val theme = KeyboardThemes.Orchid.copy(accentColor = 0xFF386F5B.toInt())
        assertEquals(theme.accentColor, SpeechPanelStyle.from(theme).orbTint)
    }

    private fun assertReadable(style: SpeechPanelStyle) {
        listOf(style.primaryLabel to style.primarySurface,
            style.secondaryLabel to style.secondarySurface).forEach { (label, surface) ->
            assertTrue(ContrastRatio.between(label, 0, surface) >= 4.5)
        }
    }
}
