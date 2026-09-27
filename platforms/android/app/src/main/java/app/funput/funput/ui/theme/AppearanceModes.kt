package app.funput.funput.ui.theme

import app.funput.funput.ime.settings.AppearanceMode

/** Whether the app draws dark: the user's choice, or the system's when they left it to the system. */
internal fun AppearanceMode.resolveDarkTheme(systemDarkTheme: Boolean): Boolean = when (this) {
    AppearanceMode.SYSTEM -> systemDarkTheme
    AppearanceMode.LIGHT -> false
    AppearanceMode.DARK -> true
}
