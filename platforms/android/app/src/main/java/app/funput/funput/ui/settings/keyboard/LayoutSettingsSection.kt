package app.funput.funput.ui.settings.keyboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.keyboard.layout.KeyboardSizingProfile
import app.funput.funput.keyboard.model.KeyboardInputMethod
import app.funput.funput.ui.settings.components.SettingsPercentSliderRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow

@Composable
internal fun LayoutSettingsSection(
    inputMethod: KeyboardInputMethod,
    showsNumberRow: Boolean,
    keySizeProfile: KeyboardSizingProfile,
    onShowsNumberRowChanged: (Boolean) -> Unit,
    onKeySizeSelected: (KeyboardSizingProfile) -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_layout),
        rows = buildList {
            if (inputMethod.isTelexFamily) {
                add { position ->
                    SettingsSwitchRow(
                        position = position,
                        title = stringResource(R.string.settings_number_row_title),
                        summary = stringResource(R.string.settings_number_row_summary),
                        checked = showsNumberRow,
                        iconRes = R.drawable.ic_keyboard,
                        onCheckedChange = onShowsNumberRowChanged,
                    )
                }
            }
            add { position ->
                SettingsPercentSliderRow(
                    position = position,
                    title = stringResource(R.string.settings_key_size_title),
                    iconRes = R.drawable.ic_key_size,
                    value = keySizeProfile.heightScale,
                    range = KeyboardSizingProfile.MinScale..KeyboardSizingProfile.MaxScale,
                    onValueSettled = { scale ->
                        onKeySizeSelected(KeyboardSizingProfile.scaled(scale))
                    },
                )
            }
        },
        modifier = Modifier.testTag(LayoutSettingsSectionTag),
    )
}

internal const val LayoutSettingsSectionTag = "settings-section-layout"
