package app.funput.funput.ui.shortcuts.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import app.funput.funput.R
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.ui.shortcuts.ShortcutsScreenModel
import app.funput.funput.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ShortcutEditorSheet(
    model: ShortcutsScreenModel,
    original: TextShortcut,
    dismiss: () -> Unit,
) {
    var draft by remember(original) { mutableStateOf(original) }
    var discardDialog by remember { mutableStateOf(false) }
    var deleteDialog by remember { mutableStateOf(false) }
    val editing = model.contains(original)
    val hasChanges = draft != original
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun requestDismiss() {
        if (model.isSaving) return
        if (hasChanges) discardDialog = true else dismiss()
    }
    ModalBottomSheet(
        onDismissRequest = ::requestDismiss,
        sheetState = sheetState,
        sheetGesturesEnabled = !hasChanges && !model.isSaving,
        modifier = Modifier.fillMaxHeight(),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(Spacing.Large),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = ::requestDismiss, enabled = !model.isSaving) {
                    Text(stringResource(R.string.shortcuts_cancel))
                }
                Text(if (editing) stringResource(R.string.shortcuts_edit)
                    else stringResource(R.string.shortcuts_add_title),
                    style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = { model.save(draft, dismiss) }, enabled = model.canWrite &&
                    draft.isValid && !model.isDuplicate(draft)) {
                    Text(stringResource(R.string.shortcuts_save))
                }
            }
            OutlinedTextField(
                value = draft.trigger,
                onValueChange = { draft = draft.copy(trigger = it) },
                label = { Text(stringResource(R.string.shortcuts_trigger)) },
                placeholder = { Text(stringResource(R.string.shortcuts_trigger_example)) },
                singleLine = true,
                isError = model.isDuplicate(draft),
                supportingText = if (model.isDuplicate(draft)) ({
                    Text(stringResource(R.string.shortcuts_duplicate))
                }) else null,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false, keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth().testTag("shortcuts-editor-trigger"),
            )
            OutlinedTextField(
                value = draft.expansion,
                onValueChange = { draft = draft.copy(expansion = it) },
                label = { Text(stringResource(R.string.shortcuts_expansion)) },
                placeholder = { Text(stringResource(R.string.shortcuts_expansion_example)) },
                minLines = 5,
                maxLines = 12,
                modifier = Modifier.fillMaxWidth().testTag("shortcuts-editor-expansion"),
            )
            Text(stringResource(R.string.shortcuts_reopen_hint),
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (editing) TextButton(onClick = { deleteDialog = true }, enabled = model.canWrite) {
                Text(stringResource(R.string.shortcuts_delete_entry), color = MaterialTheme.colorScheme.error)
            }
        }
    }
    if (discardDialog) ConfirmDiscardDialog({ discardDialog = false }, dismiss)
    if (deleteDialog) ConfirmDeleteDialog(original, { deleteDialog = false }) {
        model.delete(original, dismiss); deleteDialog = false
    }
}

@Composable
private fun ConfirmDiscardDialog(cancel: () -> Unit, discard: () -> Unit) {
    AlertDialog(onDismissRequest = cancel, title = { Text(stringResource(R.string.shortcuts_discard_title)) },
        text = { Text(stringResource(R.string.shortcuts_discard_body)) },
        confirmButton = { TextButton(onClick = discard) { Text(stringResource(R.string.shortcuts_discard)) } },
        dismissButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.shortcuts_keep_editing)) } })
}

@Composable
private fun ConfirmDeleteDialog(entry: TextShortcut, cancel: () -> Unit, delete: () -> Unit) {
    AlertDialog(onDismissRequest = cancel, title = { Text(stringResource(R.string.shortcuts_delete_title)) },
        text = { Text(stringResource(R.string.shortcuts_delete_body, entry.trigger)) },
        confirmButton = { TextButton(onClick = delete) { Text(stringResource(R.string.shortcuts_delete)) } },
        dismissButton = { TextButton(onClick = cancel) { Text(stringResource(R.string.shortcuts_cancel)) } })
}
