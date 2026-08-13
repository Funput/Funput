package app.funput.funput.ui.settings.clipboard

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.clipboard.model.ClipboardExpiry
import app.funput.funput.ui.settings.label
import app.funput.funput.ui.settings.components.SettingsDestructiveRow
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow

@Composable
internal fun ClipboardSettingsSection(
    enabled: Boolean,
    expiry: ClipboardExpiry,
    onEnabledChanged: (Boolean) -> Unit,
    onOpenExpiry: () -> Unit,
    onClear: () -> Unit,
) {
    var confirmsClear by rememberSaveable { mutableStateOf(false) }
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
            { position ->
                SettingsDestructiveRow(
                    position = position,
                    title = stringResource(R.string.settings_clipboard_clear),
                    iconRes = R.drawable.ic_delete,
                    onClick = { confirmsClear = true },
                )
            },
        ),
    )
    if (confirmsClear) ClipboardClearDialog(
        onConfirm = { confirmsClear = false; onClear() },
        onDismiss = { confirmsClear = false },
    )
}

@Composable
private fun ClipboardClearDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_clipboard_clear_dialog_title)) },
        text = {
            Text(
                text = stringResource(R.string.settings_clipboard_clear_dialog_body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.settings_clipboard_clear),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_clipboard_clear_dialog_cancel))
            }
        },
    )
}
