package app.funput.funput.ui.theme.custom

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.funput.funput.ime.settings.KeyboardThemeSelection
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.CustomKeyboardThemeStore
import app.funput.funput.theme.store.custom.CustomThemeInstaller
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the app shell needs to save and delete custom themes, built once per repository and store. */
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

/** Deletes a custom theme and repoints every slot that was using it. */
internal class CustomThemeDeleteHandler(
    private val store: CustomKeyboardThemeStore,
    private val themeSettings: KeyboardThemeSettings,
) {
    suspend fun delete(themeId: KeyboardThemeId, selection: KeyboardThemeSelection) {
        withContext(Dispatchers.IO) {
            store.deleteTheme(themeId)
        }
        // A theme can be assigned to more than one slot, and every one of them has to be
        // repointed: a slot still naming a deleted theme would silently fall back to the default
        // the next time it is resolved, which reads as the setting having been forgotten.
        selection.slotsUsing(themeId).forEach { slot ->
            themeSettings.setTheme(slot.fallbackThemeId, slot)
        }
    }
}

/** A light slot falls back to a light theme rather than to the global default. */
private val KeyboardThemeSlot.fallbackThemeId: KeyboardThemeId
    get() = when (this) {
        KeyboardThemeSlot.LIGHT -> KeyboardThemeId.Light
        KeyboardThemeSlot.DARK, KeyboardThemeSlot.SINGLE -> KeyboardThemeId.Dark
    }
