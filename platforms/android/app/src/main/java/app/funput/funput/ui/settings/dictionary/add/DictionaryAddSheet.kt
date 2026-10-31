package app.funput.funput.ui.settings.dictionary.add

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditOutcome
import app.funput.funput.ime.suggestions.dictionary.DictionaryLimits
import app.funput.funput.ui.kit.cards.FunputSection
import app.funput.funput.ui.kit.controls.FunputButton
import app.funput.funput.ui.kit.controls.FunputButtonStyle
import app.funput.funput.ui.kit.controls.FunputTextField
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.kit.overlays.FunputSheet
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.dictionary.model.DictionaryWordDraft
import app.funput.funput.ui.settings.dictionary.model.PersonalDictionaryScreenModel
import app.funput.funput.ui.settings.dictionary.model.addMessage
import kotlinx.coroutines.launch

/**
 * Adds one word by hand: a name, a brand, a nickname the bar should offer before the user has
 * ever typed it. A typed word locks the swipe-down and is confirmed before the sheet closes.
 */
@Composable
internal fun DictionaryAddSheet(model: PersonalDictionaryScreenModel, dismiss: () -> Unit) {
    var draft by remember { mutableStateOf(DictionaryWordDraft()) }
    var message by remember { mutableStateOf<Int?>(null) }
    var isSaving by remember { mutableStateOf(false) }
    var confirmsDiscard by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    val requestDismiss = { if (!isSaving) { if (draft.word.isEmpty()) dismiss() else confirmsDiscard = true } }
    val save = {
        if (draft.canSave && !isSaving) {
            isSaving = true
            scope.launch {
                val outcome = model.apply(DictionaryEditKind.ADD, draft.word)
                isSaving = false
                if (outcome == DictionaryEditOutcome.APPLIED) return@launch dismiss()
                message = outcome?.addMessage() ?: R.string.dictionary_action_error_body
                model.actionFailed = false
            }
        }
    }
    FunputSheet(onDismiss = requestDismiss, gesturesEnabled = draft.word.isEmpty() && !isSaving) {
        Column(verticalArrangement = Arrangement.spacedBy(FunputUi.spacing.large), modifier = Modifier.fillMaxWidth()) {
            Box(Modifier.fillMaxWidth()) {
                FunputButton(
                    stringResource(R.string.dictionary_cancel), requestDismiss, Modifier.align(Alignment.CenterStart),
                    style = FunputButtonStyle.PLAIN, enabled = !isSaving,
                )
                BasicText(
                    stringResource(R.string.dictionary_add),
                    style = FunputUi.typography.headline.copy(color = FunputUi.colors.label),
                    modifier = Modifier.align(Alignment.Center).semantics { heading() },
                )
                FunputButton(
                    stringResource(R.string.dictionary_add_confirm), save, Modifier.align(Alignment.CenterEnd),
                    style = FunputButtonStyle.PLAIN, enabled = draft.canSave && !isSaving,
                )
            }
            val problem = draft.problem ?: message
            FunputTextField(
                value = draft.text,
                // Typing clears the last save's complaint, which was about the word before.
                onValueChange = { draft = DictionaryWordDraft(it); message = null },
                label = stringResource(R.string.dictionary_add_field),
                placeholder = stringResource(R.string.dictionary_add_placeholder),
                error = problem?.let { stringResource(it, DictionaryLimits.MAX_WORD_LENGTH) },
                enabled = !isSaving,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.None, autoCorrectEnabled = false),
                modifier = Modifier.focusRequester(focus).testTag(DictionaryAddFieldTag),
            )
            if (problem == null) {
                BasicText(
                    stringResource(R.string.dictionary_add_hint),
                    style = FunputUi.typography.caption.copy(color = FunputUi.colors.secondaryLabel),
                )
            }
            if (draft.canSave) SuggestionPreview(draft.suggestionPreview)
        }
    }
    LaunchedEffect(focus) { focus.requestFocus() }
    if (confirmsDiscard) {
        FunputDialog(
            title = stringResource(R.string.dictionary_discard_title),
            confirmLabel = stringResource(R.string.dictionary_discard),
            onConfirm = { confirmsDiscard = false; dismiss() },
            onDismiss = { confirmsDiscard = false },
            dismissLabel = stringResource(R.string.dictionary_keep_editing),
            destructive = true,
        )
    }
}

/** The word as the suggestion bar will draw it: one slot, in the bar's own type size. */
@Composable
private fun SuggestionPreview(word: String) {
    val label = stringResource(R.string.dictionary_add_preview_label, word)
    FunputSection(title = stringResource(R.string.dictionary_add_preview)) {
        Box(Modifier.fillMaxWidth().padding(FunputUi.spacing.medium), contentAlignment = Alignment.Center) {
            BasicText(
                word,
                style = FunputUi.typography.body.copy(color = FunputUi.colors.label),
                modifier = Modifier.clip(FunputUi.shapes.capsule).background(FunputUi.colors.groupedBackground)
                    .padding(horizontal = 22.dp, vertical = 8.dp).semantics { contentDescription = label },
            )
        }
    }
}

/** Test tag of the word field. */
internal const val DictionaryAddFieldTag = "dictionary-add-field"
