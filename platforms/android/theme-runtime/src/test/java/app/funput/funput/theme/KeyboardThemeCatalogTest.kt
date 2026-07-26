package app.funput.funput.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyboardThemeCatalogTest {
    @Test
    fun localCatalogContainsBuiltInThemesInDisplayOrder() {
        assertEquals(
            listOf(
                KeyboardThemeId.Dark,
                KeyboardThemeId.Light,
                KeyboardThemeId.GlassDark,
            ),
            LocalKeyboardThemeCatalog.themes.map(KeyboardThemeDescriptor::id),
        )
        assertEquals(
            listOf(
                KeyboardThemeOrigin.BUILT_IN,
                KeyboardThemeOrigin.BUILT_IN,
                KeyboardThemeOrigin.BUILT_IN,
            ),
            LocalKeyboardThemeCatalog.themes.map(KeyboardThemeDescriptor::origin),
        )
    }

    @Test
    fun localCatalogUsesDarkAsDefault() {
        assertEquals(KeyboardThemeId.Dark, LocalKeyboardThemeCatalog.defaultTheme.id)
        assertSame(KeyboardThemes.Ink, LocalKeyboardThemeCatalog.defaultTheme.theme)
    }

    @Test
    fun resolveReturnsKnownThemeAndFallsBackForUnknownTheme() {
        assertSame(
            KeyboardThemes.Paper,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.Light).theme,
        )
        assertSame(
            KeyboardThemes.Ink,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.of("future-theme")).theme,
        )
        assertSame(
            KeyboardThemes.GlassDark,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.GlassDark).theme,
        )
    }

    @Test
    fun catalogRejectsDuplicateIdentifiers() {
        val theme = LocalKeyboardThemeCatalog.defaultTheme

        assertThrows(IllegalArgumentException::class.java) {
            KeyboardThemeCatalog(
                themes = listOf(theme, theme.copy(name = "Duplicate")),
                defaultThemeId = theme.id,
            )
        }
    }

    @Test
    fun catalogRequiresItsDefaultThemeToBePresent() {
        assertThrows(IllegalArgumentException::class.java) {
            KeyboardThemeCatalog(
                themes = listOf(LocalKeyboardThemeCatalog.defaultTheme),
                defaultThemeId = KeyboardThemeId.Light,
            )
        }
    }
}
