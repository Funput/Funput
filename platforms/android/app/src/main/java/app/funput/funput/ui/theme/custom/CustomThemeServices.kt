package app.funput.funput.ui.theme.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.store.CustomKeyboardThemeStore
import app.funput.funput.theme.store.custom.CustomThemeInstaller

internal data class CustomThemeServices(
    val saveHandler: CustomThemeSaveHandler,
    val deleteHandler: CustomThemeDeleteHandler,
)

@Composable
internal fun rememberCustomThemeServices(
    themeRepository: InstalledThemeRepository,
    customThemeStore: CustomKeyboardThemeStore,
    keyboardThemeSettings: KeyboardThemeSettings,
): CustomThemeServices = remember(themeRepository, customThemeStore, keyboardThemeSettings) {
    val installer = CustomThemeInstaller(customThemeStore)
    CustomThemeServices(
        saveHandler = CustomThemeSaveHandler(themeRepository, installer, keyboardThemeSettings),
        deleteHandler = CustomThemeDeleteHandler(customThemeStore, keyboardThemeSettings),
    )
}
