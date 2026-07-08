package app.funput.funput.ui.theme.custom

import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.CustomKeyboardThemeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class CustomThemeDeleteHandler(
    private val store: CustomKeyboardThemeStore,
    private val themeSettings: KeyboardThemeSettings,
) {
    suspend fun delete(themeId: KeyboardThemeId, selectedThemeId: KeyboardThemeId) {
        withContext(Dispatchers.IO) {
            store.deleteTheme(themeId)
        }
        if (selectedThemeId == themeId) {
            themeSettings.setTheme(KeyboardThemeId.Dark)
        }
    }
}
