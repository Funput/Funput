package app.funput.funput.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class KeyboardThemeCatalogTest {
    @Test
    fun localCatalogContainsBuiltInThemesInDisplayOrder() {
        assertEquals(
            listOf(KeyboardThemeId.Dark, KeyboardThemeId.Light),
            LocalKeyboardThemeCatalog.themes.map(KeyboardThemeDescriptor::id),
        )
    }

    @Test
    fun localCatalogUsesDarkAsDefault() {
        assertEquals(KeyboardThemeId.Dark, LocalKeyboardThemeCatalog.defaultTheme.id)
        assertSame(KeyboardThemes.Dark, LocalKeyboardThemeCatalog.defaultTheme.theme)
    }

    @Test
    fun resolveReturnsKnownThemeAndFallsBackForUnknownTheme() {
        assertSame(
            KeyboardThemes.Light,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.Light).theme,
        )
        assertSame(
            KeyboardThemes.Dark,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.of("future-theme")).theme,
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
