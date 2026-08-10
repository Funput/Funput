package app.funput.funput.ui.settings.smart

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow

@Composable
internal fun SmartSettingsSection(
    smartRestoreEnabled: Boolean,
    spellCheckEnabled: Boolean,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_smart),
        rows = listOf(
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_smart_restore_title),
                    checked = smartRestoreEnabled,
                    iconRes = R.drawable.ic_globe,
                    onCheckedChange = onSmartRestoreChanged,
                )
            },
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_spell_check_title),
                    checked = spellCheckEnabled,
                    iconRes = R.drawable.ic_check,
                    onCheckedChange = onSpellCheckChanged,
                )
            },
        ),
    )
}
