package app.funput.funput.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod

@Composable
internal fun KeyboardInputMethod.label(): String = when (this) {
    KeyboardInputMethod.VNI -> stringResource(R.string.input_method_vni)
    KeyboardInputMethod.TELEX -> stringResource(R.string.input_method_telex)
}

@Composable
internal fun AppearanceMode.label(): String = when (this) {
    AppearanceMode.SYSTEM -> stringResource(R.string.settings_theme_system)
    AppearanceMode.LIGHT -> stringResource(R.string.settings_theme_light)
    AppearanceMode.DARK -> stringResource(R.string.settings_theme_dark)
}

@Composable
internal fun KeyboardSizingProfile.label(): String = when (id) {
    KeyboardSizingProfile.Compact.id -> stringResource(R.string.settings_key_size_compact)
    KeyboardSizingProfile.Large.id -> stringResource(R.string.settings_key_size_large)
    else -> stringResource(R.string.settings_key_size_normal)
}

@Composable
internal fun inputMethodOptions() = KeyboardInputMethod.entries.map { PickerOption(it, it.label()) }

@Composable
internal fun appearanceOptions() = AppearanceMode.entries.map { PickerOption(it, it.label()) }

@Composable
internal fun keySizeOptions() = KeyboardSizingProfile.Presets.map { PickerOption(it, it.label()) }
