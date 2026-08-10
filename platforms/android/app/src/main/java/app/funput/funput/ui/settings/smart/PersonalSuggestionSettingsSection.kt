package app.funput.funput.ui.settings.smart

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow

@Composable
internal fun PersonalSuggestionSettingsSection(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var confirmsReset by remember { mutableStateOf(false) }
    SettingsSection(
        title = stringResource(R.string.settings_personal_suggestions_section),
        rows = listOf(
            { position ->
                SettingsSwitchRow(
                    position = position,
                    title = stringResource(R.string.settings_personal_suggestions_title),
                    // The explanation used to sit under the group as loose text. As the row's own
                    // summary it stays attached to the switch it describes.
                    summary = stringResource(R.string.settings_personal_suggestions_description),
                    checked = enabled,
                    iconRes = R.drawable.ic_keyboard,
                    onCheckedChange = onEnabledChanged,
                )
            },
            { position ->
                SettingsRow(
                    position = position,
                    title = stringResource(R.string.settings_personal_suggestions_reset),
                    iconRes = R.drawable.ic_globe,
                    onClick = { confirmsReset = true },
                )
            },
        ),
    )
    if (confirmsReset) ResetSuggestionsDialog(
        onConfirm = { confirmsReset = false; onReset() },
        onDismiss = { confirmsReset = false },
    )
}

@Composable
private fun ResetSuggestionsDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_personal_suggestions_reset_title)) },
        text = { Text(stringResource(R.string.settings_personal_suggestions_reset_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.settings_personal_suggestions_reset_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.settings_personal_suggestions_reset_cancel)) }
        },
    )
}
