package app.funput.funput.ui.settings.appearance

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.components.SettingsDivider
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.settings.label
import app.funput.funput.ui.theme.BrandOrange
import app.funput.funput.ui.theme.BrandPink
import app.funput.funput.ui.theme.supportsDynamicColor

@Composable
internal fun AppearanceSettingsSection(
    appearanceMode: AppearanceMode,
    dynamicColorEnabled: Boolean,
    onOpenPicker: (SettingsPicker) -> Unit,
    onDynamicColorChanged: (Boolean) -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
        SettingsRow(
            title = stringResource(R.string.settings_theme_title),
            value = appearanceMode.label(),
            iconRes = R.drawable.ic_appearance,
            iconBackground = BrandPink,
            onClick = { onOpenPicker(SettingsPicker.APPEARANCE) },
        )
        // Devices before Android 12 have no wallpaper palette to read, so the row would be a
        // switch that changes nothing. Hide it rather than ship a dead control.
        if (supportsDynamicColor) {
            SettingsDivider()
            SettingsSwitchRow(
                title = stringResource(R.string.settings_dynamic_color_title),
                summary = stringResource(R.string.settings_dynamic_color_summary),
                checked = dynamicColorEnabled,
                iconRes = R.drawable.ic_dynamic_color,
                iconBackground = BrandOrange,
                onCheckedChange = onDynamicColorChanged,
            )
        }
    }
}
