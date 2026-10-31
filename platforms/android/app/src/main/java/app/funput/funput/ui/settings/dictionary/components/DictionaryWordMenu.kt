package app.funput.funput.ui.settings.dictionary.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryOrigin
import app.funput.funput.ime.suggestions.dictionary.DictionaryWord
import app.funput.funput.ui.kit.icons.FunputIcons
import app.funput.funput.ui.kit.icons.FunputListIcons
import app.funput.funput.ui.kit.overlays.FunputMenu
import app.funput.funput.ui.kit.overlays.FunputMenuItem

/** What a row can do to its word; the screen decides how (forgetting asks first). */
internal class DictionaryWordActions(
    val copy: (DictionaryWord) -> Unit,
    val block: (DictionaryWord) -> Unit,
    val unblock: (DictionaryWord) -> Unit,
    val forget: (DictionaryWord) -> Unit,
)

/** The long-press menu of a word: copy it, then the same edits its swipe offers. */
@Composable
internal fun DictionaryWordMenu(
    word: DictionaryWord,
    actions: DictionaryWordActions,
    expanded: Boolean,
    onDismiss: () -> Unit,
) {
    val copy = FunputMenuItem(stringResource(R.string.dictionary_copy), { actions.copy(word) }, FunputListIcons.Copy)
    val edits = if (word.origin == DictionaryOrigin.BLOCKED) {
        listOf(FunputMenuItem(stringResource(R.string.dictionary_unblock), { actions.unblock(word) }, FunputIcons.Restore))
    } else {
        listOf(
            FunputMenuItem(stringResource(R.string.dictionary_block_menu), { actions.block(word) }, FunputListIcons.Block),
            FunputMenuItem(
                stringResource(R.string.dictionary_delete_menu), { actions.forget(word) }, FunputIcons.Delete,
                destructive = true,
            ),
        )
    }
    FunputMenu(expanded = expanded, onDismiss = onDismiss, items = listOf(copy) + edits)
}
