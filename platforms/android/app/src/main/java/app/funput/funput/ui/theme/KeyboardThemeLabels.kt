package app.funput.funput.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.theme.KeyboardThemeDescriptor
import app.funput.funput.theme.KeyboardThemeId

@Composable
internal fun KeyboardThemeDescriptor.localizedName(): String = when (id) {
    KeyboardThemeId.Dark -> stringResource(R.string.settings_keyboard_theme_dark)
    KeyboardThemeId.Light -> stringResource(R.string.settings_keyboard_theme_light)
    else -> name
}
