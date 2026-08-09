package app.funput.funput.ui.theme

import android.os.Build
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/** Wallpaper-derived colour needs the platform palette Android 12 introduced. */
internal val supportsDynamicColor: Boolean
    get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/**
 * The scheme to draw the app with: the wallpaper palette when the device offers one and the user
 * left it on, the brand scheme otherwise.
 *
 * Both paths cover every Material role, so nothing downstream has to know which one it got.
 */
@Composable
internal fun funputColorScheme(darkTheme: Boolean, dynamicColor: Boolean): ColorScheme {
    if (!dynamicColor || !supportsDynamicColor) {
        return if (darkTheme) FunputDarkColors else FunputLightColors
    }
    val context = LocalContext.current
    return if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
}
