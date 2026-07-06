package app.funput.funput.ui.settings.appearance

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.AppearanceMode
import app.funput.funput.ui.settings.SettingsPicker
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.label
import app.funput.funput.ui.theme.BrandPink

@Composable
internal fun AppearanceSettingsSection(
    appearanceMode: AppearanceMode,
    onOpenPicker: (SettingsPicker) -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
        SettingsRow(
            title = stringResource(R.string.settings_theme_title),
            value = appearanceMode.label(),
            iconRes = R.drawable.ic_appearance,
            iconBackground = BrandPink,
            onClick = { onOpenPicker(SettingsPicker.APPEARANCE) },
        )
    }
}
