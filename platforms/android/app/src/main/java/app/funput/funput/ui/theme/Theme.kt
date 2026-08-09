package app.funput.funput.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import app.funput.funput.ime.settings.AppearanceMode

/**
 * @param dynamicColor whether to tint from the system wallpaper palette. Defaults to off so that
 *   previews and UI tests, which do not read the setting, always get the stable brand scheme.
 */
@Composable
fun FunputTheme(
    appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = appearanceMode.resolveDarkTheme(isSystemInDarkTheme())
    MaterialTheme(
        colorScheme = funputColorScheme(darkTheme = darkTheme, dynamicColor = dynamicColor),
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
