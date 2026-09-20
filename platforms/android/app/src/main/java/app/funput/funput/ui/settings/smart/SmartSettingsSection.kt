package app.funput.funput.ui.settings.smart

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.settings.SmartCompositionPreferences
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow

@Composable
internal fun SmartSettingsSection(
    preferences: SmartCompositionPreferences,
    personalSuggestionsEnabled: Boolean,
    smartGesturesEnabled: Boolean,
    onSmartRestoreChanged: (Boolean) -> Unit,
    onSpellCheckChanged: (Boolean) -> Unit,
    onAutoCapitalizeChanged: (Boolean) -> Unit,
    onPersonalSuggestionsChanged: (Boolean) -> Unit,
    onSmartGesturesChanged: (Boolean) -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_section_smart),
        rows = listOf(
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_smart_restore_title),
                    checked = preferences.smartRestoreEnabled,
                    iconRes = R.drawable.ic_globe,
                    onCheckedChange = onSmartRestoreChanged,
                )
            },
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_spell_check_title),
                    checked = preferences.spellCheckEnabled,
                    iconRes = R.drawable.ic_check,
                    onCheckedChange = onSpellCheckChanged,
                )
            },
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_auto_capitalize_title),
                    summary = stringResource(R.string.settings_auto_capitalize_summary),
                    checked = preferences.autoCapitalizeEnabled,
                    iconRes = R.drawable.ic_keyboard,
                    onCheckedChange = onAutoCapitalizeChanged,
                )
            },
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_personal_suggestions_title),
                    summary = stringResource(R.string.settings_personal_suggestions_description),
                    checked = personalSuggestionsEnabled,
                    iconRes = R.drawable.ic_keyboard,
                    onCheckedChange = onPersonalSuggestionsChanged,
                )
            },
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_smart_gestures_title),
                    summary = stringResource(R.string.settings_smart_gestures_summary),
                    checked = smartGesturesEnabled,
                    iconRes = R.drawable.ic_keyboard,
                    onCheckedChange = onSmartGesturesChanged,
                )
            },
        ),
        modifier = Modifier.testTag(SmartSettingsSectionTag),
    )
}

internal const val SmartSettingsSectionTag = "settings-section-smart"
