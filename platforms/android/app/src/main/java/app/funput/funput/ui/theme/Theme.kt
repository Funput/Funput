package app.funput.funput.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.funput.funput.ime.settings.AppearanceMode

/**
 * Material 3's expressive theme would carry a spring-based motion scheme for every component, but
 * `MaterialExpressiveTheme` is still internal to material3 1.4.0 and only public in 1.5.0-alpha.
 * Until a stable release exposes it, components Funput owns spring on their own — see
 * `SettingsRowShapes`.
 *
 * Always the brand scheme: FunputUI gives the app its own colours, so the screens still on this
 * theme must not drift to the wallpaper palette while they wait to be rebuilt.
 */
@Composable
fun FunputTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = appearanceMode.resolveDarkTheme(isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = if (darkTheme) FunputDarkColors else FunputLightColors,
        shapes = FunputShapes,
        typography = Typography,
        content = content,
    )
}

internal fun AppearanceMode.resolveDarkTheme(systemDarkTheme: Boolean): Boolean = when (this) {
    AppearanceMode.SYSTEM -> systemDarkTheme
    AppearanceMode.LIGHT -> false
    AppearanceMode.DARK -> true
}
