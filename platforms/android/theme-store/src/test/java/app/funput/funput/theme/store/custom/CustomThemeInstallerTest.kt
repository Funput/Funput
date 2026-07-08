package app.funput.funput.theme.store.custom

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.LocalKeyboardThemeCatalog
import app.funput.funput.theme.store.CustomKeyboardThemeStore
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomThemeInstallerTest {
    @Test
    fun installBuildsAndStoresCustomTheme() {
        val store = RecordingCustomThemeStore()
        val baseTheme = LocalKeyboardThemeCatalog.resolve(KeyboardThemeId.Light)
        val draft = CustomThemeDraft(name = "Ocean", baseThemeId = baseTheme.id)

        val descriptor = CustomThemeInstaller(store).install(
            draft = draft,
            baseTheme = baseTheme,
            existingThemeIds = setOf(KeyboardThemeId.Dark, KeyboardThemeId.Light),
        )

        assertEquals(KeyboardThemeId.of("custom.ocean"), descriptor.id)
        assertEquals(listOf(descriptor), store.savedThemes)
    }
}

private class RecordingCustomThemeStore : CustomKeyboardThemeStore {
    val savedThemes = mutableListOf<KeyboardThemeDescriptor>()

    override fun loadThemes(): List<KeyboardThemeDescriptor> = savedThemes

    override fun upsertTheme(theme: KeyboardThemeDescriptor) {
        savedThemes += theme
    }

    override fun deleteTheme(id: KeyboardThemeId): Boolean = false
}
