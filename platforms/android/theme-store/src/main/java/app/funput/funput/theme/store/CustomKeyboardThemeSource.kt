package app.funput.funput.theme.store

import app.funput.funput.theme.InstalledThemeSource
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeOrigin

/** Repository source backed by user-created themes saved locally. */
class CustomKeyboardThemeSource(
    private val store: CustomKeyboardThemeStore,
) : InstalledThemeSource {
    override fun loadThemes(): List<KeyboardThemeDescriptor> =
        store.loadThemes().filter { theme -> theme.origin == KeyboardThemeOrigin.CUSTOM }
}
