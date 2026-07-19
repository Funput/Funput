package app.funput.funput.ui.settings.smart

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsDivider
import app.funput.funput.ui.settings.components.SettingsRow
import app.funput.funput.ui.settings.components.SettingsSection
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.theme.BrandBlue
import app.funput.funput.ui.theme.BrandPurple

@Composable
internal fun PersonalSuggestionSettingsSection(
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
    onReset: () -> Unit,
) {
    var confirmsReset by remember { mutableStateOf(false) }
    SettingsSection(title = stringResource(R.string.settings_personal_suggestions_section)) {
        SettingsSwitchRow(
            title = stringResource(R.string.settings_personal_suggestions_title),
            checked = enabled,
            iconRes = R.drawable.ic_keyboard,
            iconBackground = BrandPurple,
            onCheckedChange = onEnabledChanged,
        )
        Text(
            text = stringResource(R.string.settings_personal_suggestions_description),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
        SettingsDivider()
        SettingsRow(
            title = stringResource(R.string.settings_personal_suggestions_reset),
            iconRes = R.drawable.ic_globe,
            iconBackground = BrandBlue,
            onClick = { confirmsReset = true },
        )
    }
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
