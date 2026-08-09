package app.funput.funput.ui.theme.custom.color

import app.funput.funput.theme.BuiltInKeyboardThemeSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeColorLinksTest {

    private val theme = BuiltInKeyboardThemeSource.loadThemes().first().theme

    @Test
    fun `a role matching its source is following it`() {
        val linked = ThemeColorRole.SpecialLabel.write(theme, ThemeColorRole.Label.read(theme))

        assertTrue(ThemeColorLinks.isAutomatic(ThemeColorRole.SpecialLabel, linked))
    }

    @Test
    fun `changing a source carries its followers`() {
        val linked = ThemeColorRole.SpecialLabel.write(theme, ThemeColorRole.Label.read(theme))
        val red = 0xFFCC0000.toInt()

        val updated = ThemeColorLinks.write(linked, ThemeColorRole.Label, red)

        // Otherwise "automatic" would stop being true the moment its source moved, leaving the
        // follower on a colour nobody chose.
        assertEquals(red, ThemeColorRole.SpecialLabel.read(updated))
    }

    @Test
    fun `a role set apart stays put when its source moves`() {
        val apart = ThemeColorRole.SpecialLabel.write(theme, 0xFF00AA00.toInt())

        val updated = ThemeColorLinks.write(apart, ThemeColorRole.Label, 0xFFCC0000.toInt())

        assertFalse(ThemeColorLinks.isAutomatic(ThemeColorRole.SpecialLabel, updated))
        assertEquals(0xFF00AA00.toInt(), ThemeColorRole.SpecialLabel.read(updated))
    }

    @Test
    fun `restoring puts a role back under its source`() {
        val apart = ThemeColorRole.SpecialLabel.write(theme, 0xFF00AA00.toInt())

        val restored = ThemeColorLinks.restoreAutomatic(apart, ThemeColorRole.SpecialLabel)

        assertTrue(ThemeColorLinks.isAutomatic(ThemeColorRole.SpecialLabel, restored))
    }

    @Test
    fun `a role with no source is never automatic`() {
        assertFalse(ThemeColorLinks.isAutomatic(ThemeColorRole.Key, theme))
        assertEquals(theme, ThemeColorLinks.restoreAutomatic(theme, ThemeColorRole.Key))
    }
}
