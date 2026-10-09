package app.funput.funput.ui.shortcuts.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import app.funput.funput.R
import app.funput.funput.shortcuts.model.TextShortcut
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.controls.FunputTextField
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.shortcuts.ShortcutsScreenModel

/**
 * The editor's content: Cancel / title / Save across the top, the trigger and its expansion, a
 * reminder that the keyboard picks changes up when reopened, and Delete for an existing entry.
 */
@Composable
internal fun ShortcutEditorForm(
    model: ShortcutsScreenModel,
    draft: TextShortcut,
    editing: Boolean,
    onDraftChange: (TextShortcut) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = FunputUi.colors
    val duplicate = model.isDuplicate(draft)
    Column(
        verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.large),
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
    ) {
        Box(Modifier.fillMaxWidth()) {
            FunputButton(
                text = stringResource(R.string.shortcuts_cancel),
                onClick = onCancel,
                style = FunputButtonStyle.PLAIN,
                enabled = !model.isSaving,
                modifier = Modifier.align(Alignment.CenterStart),
            )
            BasicText(
                text = stringResource(if (editing) R.string.shortcuts_edit else R.string.shortcuts_add_title),
                style = FunputUi.typography.headline.copy(color = colors.label),
                modifier = Modifier.align(Alignment.Center).semantics { heading() },
            )
            FunputButton(
                text = stringResource(R.string.shortcuts_save),
                onClick = onSave,
                style = FunputButtonStyle.PLAIN,
                enabled = model.canWrite && draft.isValid && !duplicate,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        FunputTextField(
            value = draft.trigger,
            onValueChange = { onDraftChange(draft.copy(trigger = it)) },
            label = stringResource(R.string.shortcuts_trigger),
            placeholder = stringResource(R.string.shortcuts_trigger_example),
            error = if (duplicate) stringResource(R.string.shortcuts_duplicate) else null,
            enabled = !model.isSaving,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
            ),
            modifier = Modifier.testTag("shortcuts-editor-trigger"),
        )
        FunputTextField(
            value = draft.expansion,
            onValueChange = { onDraftChange(draft.copy(expansion = it)) },
            label = stringResource(R.string.shortcuts_expansion),
            placeholder = stringResource(R.string.shortcuts_expansion_example),
            singleLine = false,
            minLines = 5,
            maxLines = 12,
            enabled = !model.isSaving,
            modifier = Modifier.testTag("shortcuts-editor-expansion"),
        )
        BasicText(
            text = stringResource(R.string.shortcuts_reopen_hint),
            style = FunputUi.typography.caption.copy(color = colors.secondaryLabel),
        )
        if (editing) {
            FunputButton(
                text = stringResource(R.string.shortcuts_delete_entry),
                onClick = onDelete,
                style = FunputButtonStyle.DESTRUCTIVE,
                enabled = model.canWrite,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
