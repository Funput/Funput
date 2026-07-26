package app.funput.funput.ui.theme.custom

import app.funput.funput.ime.settings.KeyboardThemeSelection
import app.funput.funput.ime.settings.KeyboardThemeSettings
import app.funput.funput.ime.settings.KeyboardThemeSlot
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.theme.store.CustomKeyboardThemeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
