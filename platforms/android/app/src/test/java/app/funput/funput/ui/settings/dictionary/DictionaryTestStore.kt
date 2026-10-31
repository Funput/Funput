package app.funput.funput.ui.settings.dictionary

import app.funput.funput.ime.suggestions.dictionary.DictionaryEditKind
import app.funput.funput.ime.suggestions.dictionary.DictionaryEditOutcome
import app.funput.funput.ime.suggestions.dictionary.DictionaryFilter
import app.funput.funput.ime.suggestions.dictionary.DictionaryOrigin
import app.funput.funput.ime.suggestions.dictionary.DictionaryPage
import app.funput.funput.ime.suggestions.dictionary.DictionaryQuery
import app.funput.funput.ime.suggestions.dictionary.DictionarySearchKey
import app.funput.funput.ime.suggestions.dictionary.DictionaryWord
import app.funput.funput.ime.suggestions.dictionary.PersonalDictionaryStoring
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred

/** An in-memory dictionary that answers like the engine would, and records what it was asked. */
internal class DictionaryTestStore(vararg words: DictionaryWord) : PersonalDictionaryStoring {
    val words = words.toMutableList()
    val queries = mutableListOf<DictionaryQuery>()
    val edits = mutableListOf<Pair<DictionaryEditKind, String>>()
    var failsLoad = false
    var failsEdit = false

    /** When set, the next load waits for it before answering. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun load(query: DictionaryQuery): DictionaryPage {
        queries += query
        gate?.also { gate = null }?.await()
        if (failsLoad) throw IOException("unreadable")
        val needle = DictionarySearchKey(query.search)
        return DictionaryPage(
            words = words.filter { it.origin == query.filter.origin && DictionarySearchKey(it.text).contains(needle) },
            counts = DictionaryFilter.entries.associateWith { filter -> words.count { it.origin == filter.origin } },
        )
    }

    override suspend fun apply(kind: DictionaryEditKind, word: String): DictionaryEditOutcome {
        if (failsEdit) throw IOException("unwritable")
        edits += kind to word
        if (word.any(Char::isWhitespace)) return DictionaryEditOutcome.INVALID
        val existing = words.firstOrNull { it.text == word }
        val next = when (kind) {
            DictionaryEditKind.ADD -> DictionaryOrigin.ADDED
            DictionaryEditKind.BLOCK -> DictionaryOrigin.BLOCKED
            DictionaryEditKind.UNBLOCK, DictionaryEditKind.FORGET -> null
        }
        if (next != null && existing?.origin == next) return DictionaryEditOutcome.UNCHANGED
        existing?.let(words::remove)
        next?.let { words += DictionaryWord(word, uses = 0, lastUsed = 0, origin = it) }
        return DictionaryEditOutcome.APPLIED
    }

    private val DictionaryFilter.origin: DictionaryOrigin
        get() = when (this) {
            DictionaryFilter.LEARNED -> DictionaryOrigin.LEARNED
            DictionaryFilter.ADDED -> DictionaryOrigin.ADDED
            DictionaryFilter.BLOCKED -> DictionaryOrigin.BLOCKED
        }
}

internal fun learned(text: String, uses: Int = 1) = DictionaryWord(text, uses, lastUsed = uses.toLong(), DictionaryOrigin.LEARNED)

internal fun added(text: String) = DictionaryWord(text, uses = 0, lastUsed = 0, DictionaryOrigin.ADDED)

internal fun blocked(text: String) = DictionaryWord(text, uses = 0, lastUsed = 0, DictionaryOrigin.BLOCKED)
