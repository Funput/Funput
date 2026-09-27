package app.funput.funput.ui.shortcuts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.shortcuts.persistence.ShortcutsStorageError
import app.funput.funput.ui.kit.controls.FunputIconButton
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.shortcuts.editor.ShortcutEditorSheet
import app.funput.funput.ui.shortcuts.options.ShortcutOptionsSheet

/**
 * Text shortcuts: a switch for the feature, a search, and the list, with options and "add" in the
 * top bar. Editing happens in a sheet; deleting and failed saves go through dialogs.
 */
@Composable
internal fun ShortcutsScreen(model: ShortcutsScreenModel, onBack: () -> Unit) {
    var editor by remember { mutableStateOf<TextShortcut?>(null) }
    var showsOptions by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TextShortcut?>(null) }
    FunputScreen(
        title = stringResource(R.string.shortcuts_title),
        onBack = onBack,
        actions = {
            FunputIconButton(
                icon = FunputIcons.More,
                contentDescription = stringResource(R.string.shortcuts_options),
                onClick = { showsOptions = true },
                enabled = model.hasLoaded && model.loadError == null,
            )
            FunputIconButton(
                icon = FunputIcons.Add,
                contentDescription = stringResource(R.string.shortcuts_add),
                onClick = { editor = TextShortcut() },
                enabled = model.canWrite,
            )
        },
    ) {
        shortcutsSections(
            model = model,
            onEdit = { editor = it },
            onAdd = { editor = TextShortcut() },
            onDelete = { pendingDelete = it },
        )
    }
    editor?.let { entry -> ShortcutEditorSheet(model, entry) { editor = null } }
    if (showsOptions) ShortcutOptionsSheet(model) { showsOptions = false }
    pendingDelete?.let { entry ->
        DeleteShortcutDialog(entry, dismiss = { pendingDelete = null }) {
            model.delete(entry)
            pendingDelete = null
        }
    }
    model.saveError?.let { error ->
        FunputDialog(
            title = stringResource(R.string.shortcuts_save_error),
            message = stringResource(
                if (error == ShortcutsStorageError.DuplicateTrigger) R.string.shortcuts_duplicate
                else R.string.shortcuts_save_error_body,
            ),
            confirmLabel = stringResource(R.string.shortcuts_close),
            onConfirm = model::clearSaveError,
            onDismiss = model::clearSaveError,
        )
    }
}

/** Asks before deleting [entry]; used by the list's swipe and the editor's delete button. */
@Composable
internal fun DeleteShortcutDialog(entry: TextShortcut, dismiss: () -> Unit, confirm: () -> Unit) {
    FunputDialog(
        title = stringResource(R.string.shortcuts_delete_title),
        message = stringResource(R.string.shortcuts_delete_body, entry.trigger),
        confirmLabel = stringResource(R.string.shortcuts_delete),
        onConfirm = confirm,
        onDismiss = dismiss,
        dismissLabel = stringResource(R.string.shortcuts_cancel),
        destructive = true,
    )
}
