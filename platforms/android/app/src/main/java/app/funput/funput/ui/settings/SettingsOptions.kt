package app.funput.funput.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.settings.components.PickerOption

@Composable
internal fun ToneStyle.label(): String = when (this) {
    ToneStyle.TRADITIONAL -> stringResource(R.string.tone_style_traditional)
    ToneStyle.MODERN -> stringResource(R.string.tone_style_modern)
}

@Composable
internal fun KeyboardInputMethod.label(): String = when (this) {
    KeyboardInputMethod.TELEX -> stringResource(R.string.input_method_telex)
    KeyboardInputMethod.TELEX_ADVANCED -> stringResource(R.string.input_method_telex_advanced)
    KeyboardInputMethod.VNI -> stringResource(R.string.input_method_vni)
}

@Composable
internal fun KeyboardInputMethod.summary(): String = when (this) {
    KeyboardInputMethod.TELEX -> stringResource(R.string.input_method_telex_summary)
    KeyboardInputMethod.TELEX_ADVANCED -> stringResource(R.string.input_method_telex_advanced_summary)
    KeyboardInputMethod.VNI -> stringResource(R.string.input_method_vni_summary)
}

@Composable
internal fun AppearanceMode.label(): String = when (this) {
    AppearanceMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    AppearanceMode.LIGHT -> stringResource(R.string.settings_theme_light)
    AppearanceMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
internal fun KeyboardThemeId.label(): String = when (this) {
    KeyboardThemeId.Light -> stringResource(R.string.settings_keyboard_theme_light)
    else -> stringResource(R.string.settings_keyboard_theme_dark)
}

@Composable
internal fun inputMethodOptions() = KeyboardInputMethod.entries.map {
    PickerOption(it, it.label(), it.summary())
}

@Composable
internal fun toneStyleOptions() = ToneStyle.entries.map { PickerOption(it, it.label()) }

@Composable
internal fun appearanceOptions() = AppearanceMode.entries.map { PickerOption(it, it.label()) }

@Composable
internal fun ClipboardExpiry.label(): String = when (this) {
    ClipboardExpiry.HOUR -> stringResource(R.string.settings_clipboard_expiry_hour)
    ClipboardExpiry.DAY -> stringResource(R.string.settings_clipboard_expiry_day)
    ClipboardExpiry.WEEK -> stringResource(R.string.settings_clipboard_expiry_week)
}

@Composable
private fun ClipboardExpiry.summary(): String = when (this) {
    ClipboardExpiry.HOUR -> stringResource(R.string.settings_clipboard_expiry_hour_summary)
    ClipboardExpiry.DAY -> stringResource(R.string.settings_clipboard_expiry_day_summary)
    ClipboardExpiry.WEEK -> stringResource(R.string.settings_clipboard_expiry_week_summary)
}

@Composable
internal fun clipboardExpiryOptions() = ClipboardExpiry.entries.map {
    PickerOption(it, it.label(), it.summary())
}
