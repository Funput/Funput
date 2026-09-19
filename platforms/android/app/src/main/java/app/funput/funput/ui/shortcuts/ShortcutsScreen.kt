package app.funput.funput.ui.shortcuts

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.shortcuts.persistence.ShortcutsStorageError
import app.funput.funput.ui.shortcuts.editor.ShortcutEditorSheet
import app.funput.funput.ui.shortcuts.options.ShortcutOptionsSheet

@Composable
internal fun ShortcutsScreen(model: ShortcutsScreenModel, onBack: () -> Unit) {
    var editor by remember { mutableStateOf<TextShortcut?>(null) }
    var showsOptions by remember { mutableStateOf(false) }
    Scaffold(topBar = {
        ShortcutsTopBar(
            canWrite = model.canWrite,
            canShowOptions = model.hasLoaded && model.loadError == null,
            onBack = onBack,
            onOptions = { showsOptions = true },
            onAdd = { editor = TextShortcut() },
        )
    }) { padding ->
        ShortcutsList(model, padding, onEdit = { editor = it }, onAdd = { editor = TextShortcut() })
    }
    editor?.let { entry -> ShortcutEditorSheet(model, entry) { editor = null } }
    if (showsOptions) ShortcutOptionsSheet(model) { showsOptions = false }
    if (model.saveError != null) {
        AlertDialog(
            onDismissRequest = model::clearSaveError,
            title = { Text(stringResource(R.string.shortcuts_save_error)) },
            text = { Text(if (model.saveError == ShortcutsStorageError.DuplicateTrigger)
                stringResource(R.string.shortcuts_duplicate)
            else stringResource(R.string.shortcuts_save_error_body)) },
            confirmButton = { TextButton(onClick = model::clearSaveError) {
                Text(stringResource(R.string.shortcuts_close))
            } },
        )
    }
}
