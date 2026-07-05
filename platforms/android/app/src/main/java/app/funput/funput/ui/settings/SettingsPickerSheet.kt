package app.funput.funput.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.settings.components.PreferencePickerSheet

@Composable
internal fun SettingsPickerSheet(
    picker: SettingsPicker?,
    inputMethod: KeyboardInputMethod,
    keySizeProfile: KeyboardSizingProfile,
    keyboardThemeId: KeyboardThemeId,
    appearanceMode: AppearanceMode,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onKeyboardThemeSelected: (KeyboardThemeId) -> Unit,
    onAppearanceSelected: (AppearanceMode) -> Unit,
    onDismiss: () -> Unit,
) {
    when (picker) {
        SettingsPicker.INPUT_METHOD -> PreferencePickerSheet(
            title = stringResource(R.string.settings_input_method_title),
            options = inputMethodOptions(),
            selected = inputMethod,
            onSelected = onInputMethodSelected,
            onDismiss = onDismiss,
        )
        SettingsPicker.KEY_SIZE -> PreferencePickerSheet(
            title = stringResource(R.string.settings_key_size_title),
            options = keySizeOptions(),
            selected = keySizeProfile,
            onSelected = onKeySizeSelected,
            onDismiss = onDismiss,
        )
        SettingsPicker.KEYBOARD_THEME -> PreferencePickerSheet(
            title = stringResource(R.string.settings_keyboard_theme_title),
            options = keyboardThemeOptions(),
            selected = keyboardThemeId,
            onSelected = onKeyboardThemeSelected,
            onDismiss = onDismiss,
        )
        SettingsPicker.APPEARANCE -> PreferencePickerSheet(
            title = stringResource(R.string.settings_theme_title),
            options = appearanceOptions(),
            selected = appearanceMode,
            onSelected = onAppearanceSelected,
            onDismiss = onDismiss,
        )
        null -> Unit
    }
}
