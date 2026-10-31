package app.funput.funput.ui.settings.dictionary.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryFilter
import app.funput.funput.ime.suggestions.dictionary.DictionaryOrigin
import app.funput.funput.ime.suggestions.dictionary.DictionarySort
import app.funput.funput.ime.suggestions.dictionary.DictionaryWord

/*
 * The words the page uses for the store's own terms, in one place so the header, the menus and
 * TalkBack never say the same thing two ways.
 */

@get:StringRes
internal val DictionaryFilter.title: Int
    get() = when (this) {
        DictionaryFilter.LEARNED -> R.string.dictionary_filter_learned
        DictionaryFilter.ADDED -> R.string.dictionary_filter_added
        DictionaryFilter.BLOCKED -> R.string.dictionary_filter_blocked
    }

@get:StringRes
internal val DictionarySort.title: Int
    get() = when (this) {
        DictionarySort.MOST_USED -> R.string.dictionary_sort_most_used
        DictionarySort.RECENT -> R.string.dictionary_sort_recent
        DictionarySort.ALPHABETICAL -> R.string.dictionary_sort_alphabetical
    }

/** The line under the word. */
@Composable
internal fun DictionaryWord.detail(): String = when (origin) {
    DictionaryOrigin.LEARNED -> stringResource(R.string.dictionary_word_uses, uses)
    DictionaryOrigin.ADDED -> stringResource(R.string.dictionary_word_added)
    DictionaryOrigin.BLOCKED -> stringResource(R.string.dictionary_word_blocked)
}
