package app.funput.funput.keyboard.ui.emoji

import app.funput.funput.theme.KeyboardThemes
import app.funput.funput.theme.validation.ContrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiPanelPaletteTest {
    @Test fun `all presets keep panel text readable`() {
        val themes = listOf(
            KeyboardThemes.Ink,
            KeyboardThemes.Paper,
            KeyboardThemes.GlassDark,
            KeyboardThemes.GlassLight,
            KeyboardThemes.Slate,
        )
        themes.forEach { theme ->
            val palette = EmojiPanelPalette.from(theme)
            assertTrue(ContrastRatio.between(palette.label, palette.searchSurface, palette.backgroundEnd) >= 3.0)
            assertTrue(
                ContrastRatio.between(
                    palette.secondaryLabel,
                    palette.searchSurface,
                    palette.backgroundEnd,
                ) >= 3.0,
            )
            assertTrue(ContrastRatio.between(palette.secondaryLabel, 0, palette.backgroundEnd) >= 3.0)
        }
    }
}
