package app.funput.funput.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ime.settings.ToneStyle
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.components.PreferencePickerSheet

@Composable
internal fun SettingsPickerSheet(
    picker: SettingsPicker?,
    inputMethod: KeyboardInputMethod,
    toneStyle: ToneStyle,
    keySizeProfile: KeyboardSizingProfile,
    clipboardExpiry: ClipboardExpiry,
    onInputMethodSelected: (KeyboardInputMethod) -> Unit,
    onToneStyleSelected: (ToneStyle) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
    onClipboardExpirySelected: (ClipboardExpiry) -> Unit,
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
        SettingsPicker.TONE_STYLE -> PreferencePickerSheet(
            title = stringResource(R.string.settings_tone_style_title),
            options = toneStyleOptions(),
            selected = toneStyle,
            onSelected = onToneStyleSelected,
            onDismiss = onDismiss,
        )
        SettingsPicker.KEY_SIZE -> PreferencePickerSheet(
            title = stringResource(R.string.settings_key_size_title),
            options = keySizeOptions(),
            selected = keySizeProfile,
            onSelected = onKeySizeSelected,
            onDismiss = onDismiss,
        )
        SettingsPicker.CLIPBOARD_EXPIRY -> PreferencePickerSheet(
            title = stringResource(R.string.settings_clipboard_expiry_title),
            options = clipboardExpiryOptions(),
            selected = clipboardExpiry,
            onSelected = onClipboardExpirySelected,
            onDismiss = onDismiss,
        )
        null -> Unit
    }
}
