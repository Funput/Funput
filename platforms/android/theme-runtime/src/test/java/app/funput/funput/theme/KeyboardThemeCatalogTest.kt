package app.funput.funput.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class KeyboardThemeCatalogTest {
    @Test
    fun defaultThemeIsDark() {
        assertSame(KeyboardThemes.Dark, KeyboardThemeCatalog.default())
    }

    @Test
    fun fromIdResolvesKnownPresetsAndFallsBackToDark() {
        assertEquals(KeyboardThemeId.DARK, KeyboardThemeId.fromId("dark"))
        assertEquals(KeyboardThemeId.LIGHT, KeyboardThemeId.fromId("light"))
        assertEquals(KeyboardThemeId.DARK, KeyboardThemeId.fromId(null))
        assertEquals(KeyboardThemeId.DARK, KeyboardThemeId.fromId("unknown"))
    }

    @Test
    fun resolveMapsIdsToThemePresets() {
        assertSame(KeyboardThemes.Dark, KeyboardThemeCatalog.resolve(KeyboardThemeId.DARK))
        assertSame(KeyboardThemes.Light, KeyboardThemeCatalog.resolve(KeyboardThemeId.LIGHT))
    }
}
