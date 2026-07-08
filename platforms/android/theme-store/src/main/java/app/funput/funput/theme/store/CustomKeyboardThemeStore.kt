package app.funput.funput.theme.store

import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

/** Storage contract for user-created keyboard themes installed on this device. */
interface CustomKeyboardThemeStore {
    fun loadThemes(): List<KeyboardThemeDescriptor>

    fun upsertTheme(theme: KeyboardThemeDescriptor)

    fun deleteTheme(id: KeyboardThemeId): Boolean
}
