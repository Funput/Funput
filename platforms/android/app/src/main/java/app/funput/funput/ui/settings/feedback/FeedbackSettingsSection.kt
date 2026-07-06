package app.funput.funput.ui.settings.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsDivider
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.theme.BrandBlue
import app.funput.funput.ui.theme.BrandOrange

@Composable
internal fun FeedbackSettingsSection(
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
) {
    SettingsSection(title = stringResource(R.string.settings_section_feedback)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_haptics_title),
            checked = hapticsEnabled,
            iconRes = R.drawable.ic_haptic,
            iconBackground = BrandBlue,
            onCheckedChange = onHapticsChanged,
        )
        SettingsDivider()
        SettingsSwitchRow(
            title = stringResource(R.string.settings_sounds_title),
            checked = soundsEnabled,
            iconRes = R.drawable.ic_sound,
            iconBackground = BrandOrange,
            onCheckedChange = onSoundsChanged,
        )
    }
}
