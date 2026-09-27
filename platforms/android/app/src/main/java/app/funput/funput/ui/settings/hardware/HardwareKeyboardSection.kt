package app.funput.funput.ui.settings.hardware

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.kit.cards.FunputDivider
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.rows.ActionRow
import app.funput.funput.ui.kit.rows.FunputRowDefaults
import app.funput.funput.ui.kit.rows.ToggleRow
import app.funput.funput.ui.kit.theme.FunputTint

/**
 * Working beside a physical keyboard: keep Funput's keys on screen, toggle it with Alt + Space,
 * and jump to Android's own physical-keyboard settings. One grey group, since it is system-level.
 */
@Composable
internal fun HardwareKeyboardSection(state: HardwareKeyboardSectionState) {
    FunputSection(
        title = stringResource(R.string.settings_section_hardware_keyboard),
        modifier = Modifier.testTag(HardwareKeyboardSettingsSectionTag),
    ) {
        ToggleRow(
            title = stringResource(R.string.settings_hardware_keyboard_show_title),
            summary = stringResource(R.string.settings_hardware_keyboard_show_summary),
            checked = state.preferences.showsSoftKeyboard,
            onCheckedChange = state.onShowsSoftKeyboardChanged,
            icon = FunputIcons.HardwareKeyboard,
            tint = FunputTint.GRAY,
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        ToggleRow(
            title = stringResource(R.string.settings_hardware_keyboard_hotkey_title),
            summary = stringResource(R.string.settings_hardware_keyboard_hotkey_summary),
            checked = state.preferences.toggleHotkeyEnabled,
            onCheckedChange = state.onToggleHotkeyChanged,
            icon = FunputIcons.Hotkey,
            tint = FunputTint.GRAY,
        )
        FunputDivider(startInset = FunputRowDefaults.IconDividerInset)
        ActionRow(
            title = stringResource(R.string.settings_hardware_keyboard_system_title),
            summary = stringResource(R.string.settings_hardware_keyboard_system_summary),
            onClick = state.onOpenSystemSettings,
            icon = FunputIcons.OpenExternal,
            tint = FunputTint.GRAY,
        )
    }
}

/** Test tag of the physical keyboard section. */
internal const val HardwareKeyboardSettingsSectionTag = "settings-section-hardware-keyboard"
