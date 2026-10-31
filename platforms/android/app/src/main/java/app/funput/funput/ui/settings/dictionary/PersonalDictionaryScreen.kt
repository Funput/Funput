package app.funput.funput.ui.settings.dictionary

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.ime.suggestions.dictionary.DictionaryWord
import app.funput.funput.ui.kit.controls.FunputIconButton
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.layout.FunputScreen
import app.funput.funput.ui.kit.overlays.FunputDialog
import app.funput.funput.ui.kit.theme.FunputUi
import app.funput.funput.ui.settings.dictionary.add.DictionaryAddSheet
import app.funput.funput.ui.settings.dictionary.components.DictionaryWordActions
import app.funput.funput.ui.settings.dictionary.model.PersonalDictionaryScreenModel
import app.funput.funput.ui.settings.dictionary.model.screenPage

/**
 * The words the suggestion bar learned, the ones added by hand, and the ones it must never
 * offer. Wiping and adding sit together in the top bar; the wipe is red, and the app only asks
 * for it: the keyboard carries it out the next time it opens.
 */
@Composable
internal fun PersonalDictionaryScreen(model: PersonalDictionaryScreenModel, settings: DictionarySettings, onBack: () -> Unit) {
    var addsWord by rememberSaveable { mutableStateOf(false) }
    var confirmsReset by remember { mutableStateOf(false) }
    var pendingForget by remember { mutableStateOf<DictionaryWord?>(null) }
    val page = model.screenPage(settings.resetPending)
    val clipboard = LocalContext.current.getSystemService(ClipboardManager::class.java)
    val actions = remember(model, clipboard) {
        DictionaryWordActions(
            copy = { clipboard?.setPrimaryClip(ClipData.newPlainText(it.text, it.text)) },
            block = { model.edit(DictionaryEditKind.BLOCK, it.text) },
            unblock = { model.edit(DictionaryEditKind.UNBLOCK, it.text) },
            forget = { pendingForget = it },
        )
    }
    FunputScreen(
        title = stringResource(R.string.dictionary_title),
        onBack = onBack,
        itemSpacing = 0.dp,
        actions = {
            FunputIconButton(
                icon = FunputIcons.Delete,
                contentDescription = stringResource(R.string.dictionary_reset),
                onClick = { confirmsReset = true },
                enabled = !settings.resetPending && page.total > 0,
                tint = FunputUi.colors.destructive,
            )
            FunputIconButton(
                icon = FunputIcons.Add,
                contentDescription = stringResource(R.string.dictionary_add),
                onClick = { addsWord = true },
                enabled = page.hasLoaded && !page.loadFailed,
            )
        },
    ) {
        personalDictionarySections(model, page, settings, actions, onAdd = { addsWord = true })
    }
    if (addsWord) DictionaryAddSheet(model) { addsWord = false }
    if (confirmsReset) {
        FunputDialog(
            title = stringResource(R.string.dictionary_reset_title),
            message = stringResource(R.string.dictionary_reset_body),
            confirmLabel = stringResource(R.string.dictionary_reset_confirm),
            onConfirm = { confirmsReset = false; settings.onReset() },
            onDismiss = { confirmsReset = false },
            dismissLabel = stringResource(R.string.dictionary_cancel),
            destructive = true,
        )
    }
    pendingForget?.let { word ->
        FunputDialog(
            title = stringResource(R.string.dictionary_delete_title, word.text),
            message = stringResource(R.string.dictionary_delete_body),
            confirmLabel = stringResource(R.string.dictionary_delete),
            onConfirm = { pendingForget = null; model.edit(DictionaryEditKind.FORGET, word.text) },
            onDismiss = { pendingForget = null },
            dismissLabel = stringResource(R.string.dictionary_cancel),
            destructive = true,
        )
    }
    // The add sheet explains its own failures; this is for edits made on the list.
    if (model.actionFailed && !addsWord) {
        FunputDialog(
            title = stringResource(R.string.dictionary_action_error),
            message = stringResource(R.string.dictionary_action_error_body),
            confirmLabel = stringResource(R.string.dictionary_close),
            onConfirm = { model.actionFailed = false },
            onDismiss = { model.actionFailed = false },
        )
    }
}
