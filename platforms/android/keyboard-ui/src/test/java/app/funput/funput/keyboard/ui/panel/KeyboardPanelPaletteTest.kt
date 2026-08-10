package app.funput.funput.keyboard.ui.panel

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.LocalKeyboardThemeCatalog
import app.funput.funput.theme.validation.ContrastRatio
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardPanelPaletteTest {
    @Test fun `all presets keep panel text readable`() {
        val themes = LocalKeyboardThemeCatalog.themes.map(KeyboardThemeDescriptor::theme)
        themes.forEach { theme ->
            val palette = KeyboardPanelPalette.from(theme)
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
