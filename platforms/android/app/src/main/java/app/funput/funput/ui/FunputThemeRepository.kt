package app.funput.funput.ui

import app.funput.funput.theme.BuiltInKeyboardThemeSource
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.store.CustomKeyboardThemeSource
import app.funput.funput.theme.store.CustomKeyboardThemeStore

internal fun installedThemeRepository(customThemeStore: CustomKeyboardThemeStore): InstalledThemeRepository =
    InstalledThemeRepository(BuiltInKeyboardThemeSource, CustomKeyboardThemeSource(customThemeStore))
