package app.funput.funput.ui.shortcuts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.ui.settings.components.SettingsGroup
import app.funput.funput.ui.settings.components.SettingsSwitchRow
import app.funput.funput.ui.shortcuts.components.ShortcutEntryRow
import app.funput.funput.ui.shortcuts.components.ShortcutsEmptyState
import app.funput.funput.ui.shortcuts.components.ShortcutsLoadStatus
import app.funput.funput.ui.theme.Spacing

@Composable
internal fun ShortcutsList(
    model: ShortcutsScreenModel,
    padding: PaddingValues,
    onEdit: (TextShortcut) -> Unit,
    onAdd: () -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<TextShortcut?>(null) }
    LazyColumn(
        contentPadding = PaddingValues(Spacing.Large, padding.calculateTopPadding() + Spacing.Small,
            Spacing.Large, padding.calculateBottomPadding() + Spacing.Section),
        verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        modifier = Modifier.fillMaxSize().testTag(ShortcutsListTag),
    ) {
        item {
            SettingsGroup(listOf({ position ->
                SettingsSwitchRow(position, stringResource(R.string.shortcuts_enabled),
                    model.library.isEnabled, R.drawable.ic_keyboard,
                    { value -> model.updateOptions { it.copy(isEnabled = value) } },
                    stringResource(R.string.shortcuts_enabled_summary), model.canWrite)
            }))
            Text(stringResource(R.string.shortcuts_reopen_hint),
                modifier = Modifier.padding(Spacing.Large, Spacing.Small))
        }
        if (model.isLoading || model.loadError != null) item {
            ShortcutsLoadStatus(model.isLoading, model.loadError != null, model::reload)
        }
        item {
            OutlinedTextField(
                value = model.query,
                onValueChange = { model.query = it },
                label = { Text(stringResource(R.string.shortcuts_search)) },
                singleLine = true,
                trailingIcon = if (model.query.isNotEmpty()) ({
                    TextButton(onClick = { model.query = "" }) {
                        Text(stringResource(R.string.shortcuts_clear_search))
                    }
                }) else null,
                modifier = Modifier.fillMaxWidth().testTag("shortcuts-search"),
            )
        }
        if (model.hasLoaded && model.loadError == null) {
            item {
                val text = if (model.query.isBlank()) R.string.shortcuts_list_count
                else R.string.shortcuts_result_count
                Text(if (text == R.string.shortcuts_list_count) {
                    stringResource(text, model.library.entries.size)
                } else stringResource(text, model.filteredEntries.size, model.library.entries.size))
            }
            when {
                model.library.entries.isEmpty() -> item {
                    ShortcutsEmptyState(stringResource(R.string.shortcuts_empty_title),
                        stringResource(R.string.shortcuts_empty_body), onAdd)
                }
                model.filteredEntries.isEmpty() -> item {
                    ShortcutsEmptyState(stringResource(R.string.shortcuts_no_result_title),
                        stringResource(R.string.shortcuts_no_result_body))
                }
                else -> items(model.filteredEntries, key = { it.id }) { entry ->
                    ShortcutEntryRow(entry, model.canWrite, { onEdit(entry) }, { pendingDelete = entry })
                }
            }
        }
    }
    pendingDelete?.let { entry -> DeleteShortcutDialog(entry, { pendingDelete = null }) {
        model.delete(entry); pendingDelete = null
    } }
}

@Composable
private fun DeleteShortcutDialog(entry: TextShortcut, dismiss: () -> Unit, confirm: () -> Unit) {
    AlertDialog(onDismissRequest = dismiss, title = { Text(stringResource(R.string.shortcuts_delete_title)) },
        text = { Text(stringResource(R.string.shortcuts_delete_body, entry.trigger)) },
        confirmButton = { TextButton(onClick = confirm) { Text(stringResource(R.string.shortcuts_delete)) } },
        dismissButton = { TextButton(onClick = dismiss) { Text(stringResource(R.string.shortcuts_cancel)) } })
}

internal const val ShortcutsListTag = "shortcuts-list"
