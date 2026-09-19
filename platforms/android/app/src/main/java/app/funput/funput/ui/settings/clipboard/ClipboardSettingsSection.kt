package app.funput.funput.ui.settings.clipboard

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ui.settings.label
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow

@Composable
internal fun ClipboardSettingsSection(
    enabled: Boolean,
    expiry: ClipboardExpiry,
    onEnabledChanged: (Boolean) -> Unit,
    onOpenExpiry: () -> Unit,
) {
    SettingsSection(
        title = stringResource(R.string.settings_clipboard_section),
        rows = listOf(
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_clipboard_enabled_title),
                    summary = stringResource(R.string.settings_clipboard_enabled_summary),
                    checked = enabled,
                    iconRes = R.drawable.ic_clipboard,
                    onCheckedChange = onEnabledChanged,
                )
            },
            { position ->
                SettingsRow(
                    position = position,
                    title = stringResource(R.string.settings_clipboard_expiry_title),
                    summary = stringResource(R.string.settings_clipboard_expiry_summary),
                    value = expiry.label(),
                    iconRes = R.drawable.ic_history,
                    onClick = onOpenExpiry,
                )
            },
        ),
        modifier = Modifier.testTag(ClipboardSettingsSectionTag),
    )
}

internal const val ClipboardSettingsSectionTag = "settings-section-clipboard"
