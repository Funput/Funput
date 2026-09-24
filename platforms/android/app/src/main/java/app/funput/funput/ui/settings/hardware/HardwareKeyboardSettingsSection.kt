package app.funput.funput.ui.settings.hardware

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow

@Composable
internal fun HardwareKeyboardSettingsSection(state: HardwareKeyboardSectionState) {
    SettingsSection(
        title = stringResource(R.string.settings_section_hardware_keyboard),
        rows = listOf(
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_hardware_keyboard_show_title),
                    summary = stringResource(R.string.settings_hardware_keyboard_show_summary),
                    checked = state.preferences.showsSoftKeyboard,
                    iconRes = R.drawable.ic_keyboard,
                    onCheckedChange = state.onShowsSoftKeyboardChanged,
                )
            },
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_hardware_keyboard_hotkey_title),
                    summary = stringResource(R.string.settings_hardware_keyboard_hotkey_summary),
                    checked = state.preferences.toggleHotkeyEnabled,
                    iconRes = R.drawable.ic_code,
                    onCheckedChange = state.onToggleHotkeyChanged,
                )
            },
            { position ->
                SettingsRow(
                    position = position,
                    title = stringResource(R.string.settings_hardware_keyboard_system_title),
                    summary = stringResource(R.string.settings_hardware_keyboard_system_summary),
                    iconRes = R.drawable.ic_settings,
                    onClick = state.onOpenSystemSettings,
                )
            },
        ),
        modifier = Modifier.testTag(HardwareKeyboardSettingsSectionTag),
    )
}

internal const val HardwareKeyboardSettingsSectionTag = "settings-section-hardware-keyboard"
