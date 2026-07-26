package app.funput.funput.ime

import android.content.Context
import android.content.res.Configuration
import app.funput.funput.ime.settings.KeyboardThemeSelection
import app.funput.funput.theme.InstalledThemeRepository
import app.funput.funput.theme.KeyboardThemeDescriptor

/** Whether the system is currently drawing in its dark appearance. */
internal fun Context.isDarkAppearance(): Boolean =
    resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK ==
        Configuration.UI_MODE_NIGHT_YES

/**
 * Picks the theme to draw with right now.
 *
 * The keyboard reads the system appearance rather than the app's own appearance setting: the IME
 * draws over whatever app the user is in, so matching the system is what makes it look at home.
 */
internal fun InstalledThemeRepository.resolveForAppearance(
    selection: KeyboardThemeSelection,
    darkAppearance: Boolean,
): KeyboardThemeDescriptor = resolve(selection.resolve(darkAppearance))
