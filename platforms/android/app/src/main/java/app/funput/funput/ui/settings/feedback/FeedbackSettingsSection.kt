package app.funput.funput.ui.settings.feedback

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsIconTone
import app.funput.funput.ui.settings.components.SettingsSwitchRow

@Composable
internal fun FeedbackSettingsSection(
    hapticsEnabled: Boolean,
    soundsEnabled: Boolean,
    onHapticsChanged: (Boolean) -> Unit,
    onSoundsChanged: (Boolean) -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_feedback),
        rows = listOf(
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_haptics_title),
                    checked = hapticsEnabled,
                    iconRes = R.drawable.ic_haptic,
                    tone = SettingsIconTone.Tertiary,
                    onCheckedChange = onHapticsChanged,
                )
            },
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_sounds_title),
                    checked = soundsEnabled,
                    iconRes = R.drawable.ic_sound,
                    tone = SettingsIconTone.Secondary,
                    onCheckedChange = onSoundsChanged,
                )
            },
        ),
    )
}
