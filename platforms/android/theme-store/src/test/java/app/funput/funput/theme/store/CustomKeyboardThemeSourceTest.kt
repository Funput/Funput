package app.funput.funput.theme.store

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.KeyboardThemeOrigin
import app.funput.funput.theme.KeyboardThemes
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomKeyboardThemeSourceTest {
    @Test
    fun sourceOnlyExposesCustomThemes() {
        val customTheme = descriptor(KeyboardThemeId.of("custom.midnight"), KeyboardThemeOrigin.CUSTOM)
        val builtInTheme = descriptor(KeyboardThemeId.of("custom.fake"), KeyboardThemeOrigin.BUILT_IN)
        val source = CustomKeyboardThemeSource(
            store = FakeCustomThemeStore(listOf(customTheme, builtInTheme)),
        )

        assertEquals(listOf(customTheme.id), source.loadThemes().map { theme -> theme.id })
    }

    private fun descriptor(
        id: KeyboardThemeId,
        origin: KeyboardThemeOrigin,
    ): KeyboardThemeDescriptor = KeyboardThemeDescriptor(
        id = id,
        version = 1,
        name = id.value,
        author = "Tester",
        origin = origin,
        theme = KeyboardThemes.Dark,
    )
}

private class FakeCustomThemeStore(
    private val themes: List<KeyboardThemeDescriptor>,
) : CustomKeyboardThemeStore {
    override fun loadThemes(): List<KeyboardThemeDescriptor> = themes

    override fun upsertTheme(theme: KeyboardThemeDescriptor) = Unit

    override fun deleteTheme(id: KeyboardThemeId): Boolean = false
}
