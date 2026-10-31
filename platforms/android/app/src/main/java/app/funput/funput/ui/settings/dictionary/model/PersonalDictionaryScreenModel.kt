package app.funput.funput.ui.settings.dictionary.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditOutcome
import app.funput.funput.ime.suggestions.dictionary.DictionaryFilter
import app.funput.funput.ime.suggestions.dictionary.DictionaryPage
import app.funput.funput.ime.suggestions.dictionary.DictionaryQuery
import app.funput.funput.ime.suggestions.dictionary.DictionarySort
import app.funput.funput.ime.suggestions.dictionary.PersonalDictionaryStoring
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The dictionary page's state: which list is showing, how it is sorted and searched, and the
 * words the store answered with.
 *
 * Owned by the page, so every visit reads the store afresh: the keyboard learns and drains
 * edits while the app is away. Only the latest query's answer is kept; one that lands after
 * the user has moved on to another list or search is dropped.
 */
internal class PersonalDictionaryScreenModel(
    private val store: PersonalDictionaryStoring,
    private val scope: CoroutineScope,
    private val searchPauseMillis: Long = SearchPauseMillis,
) {
    var page by mutableStateOf(DictionaryPage())
        private set
    var hasLoaded by mutableStateOf(false)
        private set
    var loadFailed by mutableStateOf(false)
        private set

    /** An edit made on the list that could not be saved, shown once and then cleared. */
    var actionFailed by mutableStateOf(false)
    var filter by mutableStateOf(DictionaryFilter.LEARNED)
        private set
    var sort by mutableStateOf(DictionarySort.MOST_USED)
        private set
    var search by mutableStateOf("")
        private set

    private var pendingLoad: Job? = null

    val query: DictionaryQuery get() = DictionaryQuery(filter, sort, search)

    /** The most any listed word has been used: what the frequency bars measure against. */
    val highestUses: Int get() = page.words.maxOfOrNull { it.uses } ?: 0

    fun select(filter: DictionaryFilter) {
        if (filter == this.filter) return
        this.filter = filter
        reload()
    }

    fun sortBy(sort: DictionarySort) {
        if (sort == this.sort) return
        this.sort = sort
        reload()
    }

    /** Waits for a pause in typing, so a word typed at speed is one query. */
    fun search(text: String) {
        if (text == search) return
        search = text
        schedule(searchPauseMillis)
    }

    fun reload() = schedule(0L)

    /**
     * Applies one edit and, when it changed anything, reads the lists again. The engine decides
     * whether an edit applies, under the rules the keyboard will apply it by, so its answer is
     * returned as is for the add sheet to explain. Null means the store could not be reached.
     */
    suspend fun apply(kind: DictionaryEditKind, word: String): DictionaryEditOutcome? {
        val outcome = try {
            store.apply(kind, word)
        } catch (_: IOException) {
            actionFailed = true
            return null
        }
        if (outcome == DictionaryEditOutcome.APPLIED) {
            pendingLoad?.cancel()
            load()
        }
        return outcome
    }

    /** An edit from the list itself, whose outcome only matters when it fails. */
    fun edit(kind: DictionaryEditKind, word: String) {
        scope.launch { apply(kind, word) }
    }

    private fun schedule(pauseMillis: Long) {
        pendingLoad?.cancel()
        pendingLoad = scope.launch {
            if (pauseMillis > 0L) delay(pauseMillis)
            load()
        }
    }

    private suspend fun load() {
        val asked = query
        try {
            val answer = store.load(asked)
            // A newer list, sort or search was asked for meanwhile; its own load will land.
            if (asked != query) return
            page = answer
            loadFailed = false
        } catch (_: IOException) {
            loadFailed = true
        }
        hasLoaded = true
    }

    companion object {
        /** Long enough to wait out a word typed at speed, short enough to feel live. */
        const val SearchPauseMillis = 150L
    }
}
