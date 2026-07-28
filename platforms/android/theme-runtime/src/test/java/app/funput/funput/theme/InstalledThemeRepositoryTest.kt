package app.funput.funput.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class InstalledThemeRepositoryTest {
    @Test
    fun builtInRepositoryExposesBundledThemesInDisplayOrder() {
        val repository = InstalledThemeRepository.builtIn()

        assertEquals(
            listOf(
                KeyboardThemeId.Dark,
                KeyboardThemeId.Light,
                KeyboardThemeId.GlassDark,
                KeyboardThemeId.GlassLight,
                KeyboardThemeId.Slate,
            ),
            repository.themes.map(KeyboardThemeDescriptor::id),
        )
        assertSame(KeyboardThemes.Ink, repository.defaultTheme.theme)
    }

    @Test
    fun repositoryMergesSourcesAndResolvesUnknownThemeToDefault() {
        val customThemeId = KeyboardThemeId.of("custom.sunset")
        val repository = InstalledThemeRepository(
            BuiltInKeyboardThemeSource,
            InstalledThemeSource { listOf(customTheme(customThemeId)) },
        )

        assertEquals(customThemeId, repository.find(customThemeId)?.id)
        assertEquals(KeyboardThemeId.Dark, repository.resolve(KeyboardThemeId.of("missing")).id)
    }

    @Test
    fun repositoryReadsLatestThemesFromSources() {
        val customThemeId = KeyboardThemeId.of("custom.ocean")
        var installedThemes = emptyList<KeyboardThemeDescriptor>()
        val repository = InstalledThemeRepository(
            BuiltInKeyboardThemeSource,
            InstalledThemeSource { installedThemes },
        )

        assertEquals(null, repository.find(customThemeId))

        installedThemes = listOf(customTheme(customThemeId))

        assertEquals(customThemeId, repository.find(customThemeId)?.id)
    }

    @Test
    fun snapshotReadsSourcesOnceAndThenStopsTouchingThem() {
        var loads = 0
        val repository = InstalledThemeRepository(
            BuiltInKeyboardThemeSource,
            InstalledThemeSource {
                loads += 1
                emptyList()
            },
        )

        val catalog = repository.snapshot()
        repeat(5) { catalog.resolve(KeyboardThemeId.Dark) }

        assertEquals(1, loads)
    }

    @Test
    fun eachDirectAccessorStillReReadsSoSeparateProcessesCannotGoStale() {
        var loads = 0
        val repository = InstalledThemeRepository(
            BuiltInKeyboardThemeSource,
            InstalledThemeSource {
                loads += 1
                emptyList()
            },
        )

        repository.resolve(KeyboardThemeId.Dark)
        repository.resolve(KeyboardThemeId.Dark)

        assertEquals(2, loads)
    }

    @Test
    fun repositoryRejectsDuplicateThemeIdentifiersAcrossSources() {
        val repository = InstalledThemeRepository(
            BuiltInKeyboardThemeSource,
            InstalledThemeSource { listOf(customTheme(KeyboardThemeId.Dark)) },
        )

        assertThrows(IllegalArgumentException::class.java) {
            repository.themes
        }
    }

    @Test
    fun backgroundImageRequiresSafeOpacity() {
        assertThrows(IllegalArgumentException::class.java) {
            KeyboardThemeBackgroundImage(source = "content://theme/background", opacity = 1.1f)
        }
    }

    private fun customTheme(id: KeyboardThemeId): KeyboardThemeDescriptor =
        KeyboardThemeDescriptor(
            id = id,
            version = 1,
            name = "Sunset",
            author = "Tester",
            origin = KeyboardThemeOrigin.CUSTOM,
            backgroundImage = KeyboardThemeBackgroundImage(
                source = "content://funput-themes/sunset",
                opacity = 0.72f,
            ),
            theme = KeyboardThemes.Paper,
        )
}
