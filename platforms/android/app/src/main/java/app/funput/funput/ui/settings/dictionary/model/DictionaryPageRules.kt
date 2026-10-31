package app.funput.funput.ui.settings.dictionary.model

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import app.funput.funput.R
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditOutcome
import app.funput.funput.ime.suggestions.dictionary.DictionaryFilter
import app.funput.funput.ime.suggestions.dictionary.DictionaryLimits
import app.funput.funput.ime.suggestions.dictionary.DictionarySort
import app.funput.funput.ime.suggestions.dictionary.DictionaryWord
import java.util.Locale

/**
 * What the page draws, read once by the screen and handed to the lazy list rather than read
 * inside its items, as on the shortcuts page.
 *
 * While a wipe waits for the keyboard, the learned and added words are as good as gone, so
 * they are not shown: listing them would invite edits to words about to disappear.
 */
@Immutable
internal data class DictionaryScreenPage(
    val filter: DictionaryFilter,
    val sort: DictionarySort,
    val search: String,
    val hasLoaded: Boolean,
    val loadFailed: Boolean,
    val words: List<DictionaryWord>,
    val highestUses: Int,
    private val counts: Map<DictionaryFilter, Int>,
) {
    fun count(filter: DictionaryFilter): Int = counts[filter] ?: 0

    /** The words the dictionary offers: learned and added, not the blocked ones. */
    val total: Int get() = count(DictionaryFilter.LEARNED) + count(DictionaryFilter.ADDED)

    val isSearching: Boolean get() = search.isNotEmpty()
}

internal fun PersonalDictionaryScreenModel.screenPage(resetPending: Boolean): DictionaryScreenPage {
    fun hidden(filter: DictionaryFilter) = resetPending && filter != DictionaryFilter.BLOCKED
    return DictionaryScreenPage(
        filter = filter,
        sort = sort,
        search = search,
        hasLoaded = hasLoaded,
        loadFailed = loadFailed,
        words = if (hidden(filter)) emptyList() else page.words,
        highestUses = highestUses,
        counts = DictionaryFilter.entries.associateWith { if (hidden(it)) 0 else page.count(it) },
    )
}

/**
 * The word being typed into the add sheet, and what is wrong with it so far. These are only the
 * checks worth showing while typing; the engine has the last word when the draft is saved.
 */
@Immutable
internal data class DictionaryWordDraft(val text: String = "") {
    val word: String get() = text.trim()

    @get:StringRes
    val problem: Int?
        get() = when {
            word.any(Char::isWhitespace) -> R.string.dictionary_add_spaced
            word.codePointCount(0, word.length) > DictionaryLimits.MAX_WORD_LENGTH -> R.string.dictionary_add_too_long
            else -> null
        }

    val canSave: Boolean get() = word.isNotEmpty() && problem == null

    /** The spelling the bar will offer: the store keeps words lowercase. */
    val suggestionPreview: String get() = word.lowercase(Locale.ROOT)
}

/** Why a save did not close the add sheet, or null when it should close. */
@StringRes
internal fun DictionaryEditOutcome.addMessage(): Int? = when (this) {
    DictionaryEditOutcome.APPLIED -> null
    DictionaryEditOutcome.UNCHANGED -> R.string.dictionary_add_unchanged
    DictionaryEditOutcome.LIMIT_REACHED -> R.string.dictionary_add_limit
    DictionaryEditOutcome.INVALID -> R.string.dictionary_add_invalid
}
