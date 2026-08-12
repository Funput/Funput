package app.funput.funput.keyboard.rendering

import app.funput.funput.theme.KeyboardThemes
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardChipRendererTest {
    @Test
    fun `all preset accents receive readable paste foreground`() {
        val themes = listOf(
            KeyboardThemes.Slate,
            KeyboardThemes.Ink,
            KeyboardThemes.Paper,
            KeyboardThemes.GlassDark,
            KeyboardThemes.GlassLight,
            KeyboardThemes.Blossom,
            KeyboardThemes.Orchid,
        )
        themes.forEach { theme ->
            val contrast = clipboardContrast(
                clipboardForeground(theme.accentColor),
                theme.accentColor,
            )
            assertTrue("Clipboard contrast was $contrast", contrast >= 4.5)
        }
    }
}
