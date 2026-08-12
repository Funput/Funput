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
            val label = palette.readable(palette.label)
            val secondary = palette.readable(palette.secondaryLabel)
            assertTrue(ContrastRatio.between(label, 0, palette.backgroundStart) >= 4.5)
            assertTrue(ContrastRatio.between(label, 0, palette.backgroundEnd) >= 4.5)
            assertTrue(ContrastRatio.between(secondary, 0, palette.backgroundStart) >= 4.5)
            assertTrue(ContrastRatio.between(secondary, 0, palette.backgroundEnd) >= 4.5)
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

    @Test fun `clipboard foreground falls back on extreme custom backgrounds`() {
        val dark = contrastForeground(0xff050505.toInt(), intArrayOf(0xff000000.toInt()), 4.5)
        val light = contrastForeground(0xfffafafa.toInt(), intArrayOf(0xffffffff.toInt()), 4.5)
        assertTrue(ContrastRatio.between(dark, 0, 0xff000000.toInt()) >= 4.5)
        assertTrue(ContrastRatio.between(light, 0, 0xffffffff.toInt()) >= 4.5)
    }
}
