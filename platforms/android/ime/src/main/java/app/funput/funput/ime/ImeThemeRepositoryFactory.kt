package app.funput.funput.ime

import android.content.Context
import app.funput.funput.theme.BuiltInKeyboardThemeSource
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.store.CustomKeyboardThemeSource
import app.funput.funput.theme.store.customKeyboardThemeStore

internal fun Context.installedImeThemeRepository(): InstalledThemeRepository =
    InstalledThemeRepository(
        BuiltInKeyboardThemeSource,
        CustomKeyboardThemeSource(customKeyboardThemeStore()),
    )
