package app.funput.funput.ui.shortcuts.options

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ui.settings.components.SettingsGroup
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.shortcuts.ShortcutsScreenModel
import app.funput.funput.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShortcutOptionsSheet(model: ShortcutsScreenModel, dismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = { if (!model.isSaving) dismiss() }) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            modifier = Modifier.fillMaxWidth().padding(Spacing.Large)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.shortcuts_options_title))
                TextButton(onClick = dismiss, enabled = !model.isSaving) {
                    Text(stringResource(R.string.shortcuts_done))
                }
            }
            SettingsGroup(listOf(
                { position -> SettingsSwitchRow(position,
                    stringResource(R.string.shortcuts_smart_case), model.library.smartCase,
                    R.drawable.ic_settings, { value ->
                        model.updateOptions { it.copy(smartCase = value) }
                    }, stringResource(R.string.shortcuts_smart_case_summary), model.canWrite) },
                { position -> SettingsSwitchRow(position,
                    stringResource(R.string.shortcuts_in_english), model.library.inEnglish,
                    R.drawable.ic_globe, { value ->
                        model.updateOptions { it.copy(inEnglish = value) }
                    }, stringResource(R.string.shortcuts_in_english_summary), model.canWrite) },
            ))
            Text(stringResource(R.string.shortcuts_reopen_hint))
        }
    }
}
