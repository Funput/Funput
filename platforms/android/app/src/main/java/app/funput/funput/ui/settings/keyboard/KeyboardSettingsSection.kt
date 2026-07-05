package app.funput.funput.ui.settings.keyboard

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.theme.KeyboardThemeId
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.components.SettingsDivider
import app.funput.funput.ui.settings.components.SettingsGroup
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSectionHeader
import app.funput.funput.ui.settings.label
import app.funput.funput.ui.settings.setup.KeyboardSetupStatus
import app.funput.funput.ui.theme.BrandBlue
import app.funput.funput.ui.theme.BrandOrange
import app.funput.funput.ui.theme.BrandPurple

@Composable
internal fun KeyboardSettingsSection(
    setupStatus: KeyboardSetupStatus,
    inputMethod: KeyboardInputMethod,
    keySizeProfile: KeyboardSizingProfile,
    keyboardThemeId: KeyboardThemeId,
    onOpenPicker: (SettingsPicker) -> Unit,
    onEnableKeyboard: () -> Unit,
    onSelectKeyboard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SettingsSectionHeader(stringResource(R.string.settings_section_keyboard))
        KeyboardSetupCard(
            status = setupStatus,
            onEnableKeyboard = onEnableKeyboard,
            onSelectKeyboard = onSelectKeyboard,
        )
        Spacer(modifier = Modifier.height(14.dp))
        SettingsSectionHeader(stringResource(R.string.settings_keyboard_customize_heading))
        SettingsGroup {
            SettingsRow(
                title = stringResource(R.string.settings_input_method_title),
                value = inputMethod.label(),
                iconRes = R.drawable.ic_keyboard,
                iconBackground = BrandPurple,
                onClick = { onOpenPicker(SettingsPicker.INPUT_METHOD) },
            )
            SettingsDivider()
            SettingsRow(
                title = stringResource(R.string.settings_key_size_title),
                value = keySizeProfile.label(),
                iconRes = R.drawable.ic_key_size,
                iconBackground = BrandOrange,
                onClick = { onOpenPicker(SettingsPicker.KEY_SIZE) },
            )
            SettingsDivider()
            SettingsRow(
                title = stringResource(R.string.settings_keyboard_theme_title),
                value = keyboardThemeId.label(),
                iconRes = R.drawable.ic_keyboard_theme,
                iconBackground = BrandBlue,
                onClick = { onOpenPicker(SettingsPicker.KEYBOARD_THEME) },
            )
        }
    }
}
