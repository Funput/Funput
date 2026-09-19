package app.funput.funput.ui.settings.data

import androidx.annotation.StringRes
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsDestructiveRow
import app.funput.funput.ui.settings.components.SettingsSection

@Composable
internal fun DataSettingsSection(
    onResetPersonalSuggestions: () -> Unit,
    onClearClipboardHistory: () -> Unit,
) {
    var pendingAction by rememberSaveable { mutableStateOf<DataAction?>(null) }
    SettingsSection(
        title = stringResource(R.string.settings_section_data),
        rows = listOf(
            { position ->
                SettingsDestructiveRow(
                    position = position,
                    title = stringResource(R.string.settings_data_suggestions_title),
                    summary = stringResource(R.string.settings_data_suggestions_summary),
                    iconRes = R.drawable.ic_globe,
                    onClick = { pendingAction = DataAction.SUGGESTIONS },
                )
            },
            { position ->
                SettingsDestructiveRow(
                    position = position,
                    title = stringResource(R.string.settings_data_clipboard_title),
                    summary = stringResource(R.string.settings_data_clipboard_summary),
                    iconRes = R.drawable.ic_clipboard,
                    onClick = { pendingAction = DataAction.CLIPBOARD },
                )
            },
        ),
        modifier = Modifier.testTag(DataSettingsSectionTag),
    )
    pendingAction?.let { action ->
        DataConfirmationDialog(
            action = action,
            onConfirm = {
                pendingAction = null
                when (action) {
                    DataAction.SUGGESTIONS -> onResetPersonalSuggestions()
                    DataAction.CLIPBOARD -> onClearClipboardHistory()
                }
            },
            onDismiss = { pendingAction = null },
        )
    }
}

@Composable
private fun DataConfirmationDialog(
    action: DataAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val copy = action.confirmationCopy()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(copy.title)) },
        text = {
            Text(
                text = stringResource(copy.body),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(copy.confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.settings_clipboard_clear_dialog_cancel))
            }
        },
    )
}

private enum class DataAction {
    SUGGESTIONS,
    CLIPBOARD,
}

private fun DataAction.confirmationCopy() = when (this) {
    DataAction.SUGGESTIONS -> ConfirmationCopy(
        R.string.settings_personal_suggestions_reset_title,
        R.string.settings_personal_suggestions_reset_body,
        R.string.settings_personal_suggestions_reset_confirm,
    )
    DataAction.CLIPBOARD -> ConfirmationCopy(
        R.string.settings_clipboard_clear_dialog_title,
        R.string.settings_clipboard_clear_dialog_body,
        R.string.settings_clipboard_clear,
    )
}

private data class ConfirmationCopy(
    @StringRes val title: Int,
    @StringRes val body: Int,
    @StringRes val confirm: Int,
)

internal const val DataSettingsSectionTag = "settings-section-data"
