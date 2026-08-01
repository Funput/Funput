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
                KeyboardThemeId.GlassLight,
                KeyboardThemeId.Slate,
                KeyboardThemeId.Blossom,
                KeyboardThemeId.Orchid,
            ),
            LocalKeyboardThemeCatalog.themes.map(KeyboardThemeDescriptor::id),
        )
        assertEquals(
            List(7) { KeyboardThemeOrigin.BUILT_IN },
            LocalKeyboardThemeCatalog.themes.map(KeyboardThemeDescriptor::origin),
        )
    }

    @Test
    fun localCatalogUsesDarkAsDefault() {
        assertEquals(KeyboardThemeId.Dark, LocalKeyboardThemeCatalog.defaultTheme.id)
        assertSame(KeyboardThemes.Ink, LocalKeyboardThemeCatalog.defaultTheme.theme)
    }

    @Test
    fun bundledThemesShareTheIosKeyShape() {
        LocalKeyboardThemeCatalog.themes.forEach { descriptor ->
            assertEquals(6f, descriptor.theme.keyCornerRadiusDp, 0f)
            assertEquals(0f, descriptor.theme.keyBorderWidthDp, 0f)
            assertEquals(0, descriptor.theme.keyBorderColor ushr 24)
        }
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
        assertSame(
            KeyboardThemes.GlassLight,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.GlassLight).theme,
        )
        assertSame(
            KeyboardThemes.Slate,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.Slate).theme,
        )
        assertSame(
            KeyboardThemes.Blossom,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.Blossom).theme,
        )
        assertSame(
            KeyboardThemes.Orchid,
            LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.Orchid).theme,
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
