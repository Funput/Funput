package app.funput.funput.theme.store

import app.funput.funput.theme.KeyboardThemeBackgroundImage
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.KeyboardThemes
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CustomThemeFileStoreTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun upsertThemePersistsCustomThemeDescriptor() {
        val store = CustomThemeFileStore(temporaryFolder.newFolder("themes"))
        val theme = customTheme(KeyboardThemeId.of("custom.sunset"), "Sunset")

        store.upsertTheme(theme)

        val loadedTheme = store.loadThemes().single()
        assertEquals(theme.id, loadedTheme.id)
        assertEquals(theme.name, loadedTheme.name)
        assertEquals(theme.backgroundImage, loadedTheme.backgroundImage)
        assertEquals(theme.theme, loadedTheme.theme)
    }

    @Test
    fun loadThemesReturnsCustomThemesInStableDisplayOrder() {
        val store = CustomThemeFileStore(temporaryFolder.newFolder("themes"))
        val ocean = customTheme(KeyboardThemeId.of("custom.ocean"), "Ocean")
        val sunset = customTheme(KeyboardThemeId.of("custom.sunset"), "Sunset")

        store.upsertTheme(sunset)
        store.upsertTheme(ocean)

        assertEquals(listOf(ocean.id, sunset.id), store.loadThemes().map { theme -> theme.id })
    }

    @Test
    fun deleteThemeRemovesPersistedTheme() {
        val store = CustomThemeFileStore(temporaryFolder.newFolder("themes"))
        val theme = customTheme(KeyboardThemeId.of("custom.ocean"), "Ocean")
        store.upsertTheme(theme)

        assertTrue(store.deleteTheme(theme.id))

        assertEquals(emptyList<KeyboardThemeDescriptor>(), store.loadThemes())
    }

    @Test
    fun loadThemesSkipsCorruptThemeFiles() {
        val directory = temporaryFolder.newFolder("themes")
        File(directory, "broken.properties").writeText("not-a-theme=true")

        assertEquals(emptyList<KeyboardThemeDescriptor>(), CustomThemeFileStore(directory).loadThemes())
    }

    @Test
    fun upsertThemeRejectsNonCustomTheme() {
        val store = CustomThemeFileStore(temporaryFolder.newFolder("themes"))
        val builtInTheme = customTheme(KeyboardThemeId.of("built.in"), "Built in")
            .copy(origin = KeyboardThemeOrigin.BUILT_IN)

        assertThrows(IllegalArgumentException::class.java) {
            store.upsertTheme(builtInTheme)
        }
    }

    private fun customTheme(id: KeyboardThemeId, name: String): KeyboardThemeDescriptor =
        KeyboardThemeDescriptor(
            id = id,
            version = 1,
            name = name,
            author = "Funput Tester",
            origin = KeyboardThemeOrigin.CUSTOM,
            backgroundImage = KeyboardThemeBackgroundImage(
                source = "content://funput-themes/${id.value}",
                opacity = 0.64f,
            ),
            theme = KeyboardThemes.Paper,
        )
}
