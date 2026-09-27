package app.funput.funput.ui.shortcuts.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.shortcuts.DeleteShortcutDialog
import app.funput.funput.ui.shortcuts.ShortcutsScreenModel

/**
 * Adds or edits one shortcut. Starts from [original] (a blank [TextShortcut] to add one). Unsaved
 * changes lock the swipe-down and are confirmed before the sheet closes; saving and deleting go
 * through [model] and close the sheet once they land.
 */
@Composable
internal fun ShortcutEditorSheet(
    model: ShortcutsScreenModel,
    original: TextShortcut,
    dismiss: () -> Unit,
) {
    var draft by remember(original) { mutableStateOf(original) }
    var confirmsDiscard by remember { mutableStateOf(false) }
    var confirmsDelete by remember { mutableStateOf(false) }
    val hasChanges = draft != original
    val requestDismiss = {
        if (!model.isSaving) {
            if (hasChanges) confirmsDiscard = true else dismiss()
        }
    }
    FunputSheet(onDismiss = requestDismiss, gesturesEnabled = !hasChanges && !model.isSaving) {
        ShortcutEditorForm(
            model = model,
            draft = draft,
            editing = model.contains(original),
            onDraftChange = { draft = it },
            onCancel = requestDismiss,
            onSave = { model.save(draft, dismiss) },
            onDelete = { confirmsDelete = true },
        )
    }
    if (confirmsDiscard) {
        FunputDialog(
            title = stringResource(R.string.shortcuts_discard_title),
            message = stringResource(R.string.shortcuts_discard_body),
            confirmLabel = stringResource(R.string.shortcuts_discard),
            onConfirm = {
                confirmsDiscard = false
                dismiss()
            },
            onDismiss = { confirmsDiscard = false },
            dismissLabel = stringResource(R.string.shortcuts_keep_editing),
            destructive = true,
        )
    }
    if (confirmsDelete) {
        DeleteShortcutDialog(original, dismiss = { confirmsDelete = false }) {
            confirmsDelete = false
            model.delete(original, dismiss)
        }
    }
}
