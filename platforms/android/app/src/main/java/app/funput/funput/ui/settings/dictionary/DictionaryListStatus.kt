package app.funput.funput.ui.settings.dictionary

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryFilter
import app.funput.funput.ui.kit.cards.FunputMessageCard
import app.funput.funput.ui.settings.dictionary.model.DictionaryScreenPage

/**
 * What stands where the words would be: the store still loading or failing to open, a search
 * that matched nothing, or an empty list with an invitation of its own.
 */
@Composable
internal fun DictionaryListStatus(page: DictionaryScreenPage, retry: () -> Unit, clearSearch: () -> Unit, add: () -> Unit) {
    when {
        page.loadFailed -> FunputMessageCard(
            title = stringResource(R.string.dictionary_load_error),
            actionLabel = stringResource(R.string.dictionary_retry),
            onAction = retry,
        )
        !page.hasLoaded -> FunputMessageCard(title = stringResource(R.string.dictionary_loading))
        page.isSearching -> FunputMessageCard(
            title = stringResource(R.string.dictionary_empty_search_title),
            body = stringResource(R.string.dictionary_empty_search_body),
            actionLabel = stringResource(R.string.dictionary_clear_search),
            onAction = clearSearch,
        )
        else -> when (page.filter) {
            DictionaryFilter.LEARNED -> FunputMessageCard(
                title = stringResource(R.string.dictionary_empty_learned_title),
                body = stringResource(R.string.dictionary_empty_learned_body),
            )
            DictionaryFilter.ADDED -> FunputMessageCard(
                title = stringResource(R.string.dictionary_empty_added_title),
                body = stringResource(R.string.dictionary_empty_added_body),
                actionLabel = stringResource(R.string.dictionary_add),
                onAction = add,
            )
            DictionaryFilter.BLOCKED -> FunputMessageCard(
                title = stringResource(R.string.dictionary_empty_blocked_title),
                body = stringResource(R.string.dictionary_empty_blocked_body),
            )
        }
    }
}
