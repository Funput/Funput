package app.funput.funput.ime.suggestions.dictionary

/** How a word came to be in the dictionary. */
enum class DictionaryOrigin { LEARNED, ADDED, BLOCKED }

/** The four changes a person can make to one word. */
enum class DictionaryEditKind {
    /** Offered at once and never evicted. Lifts a block on the word. */
    ADD,

    /** Dropped with what it learned; typing it again relearns it. */
    FORGET,

    /** Dropped and never learned or offered again. */
    BLOCK,
    UNBLOCK,
}

/** What an edit did, as the engine reports it. */
enum class DictionaryEditOutcome {
    APPLIED,

    /** The dictionary already looked like this. */
    UNCHANGED,

    /** Not a word the store can hold: empty, spaced or too long. */
    INVALID,

    /** The added-word or blocked-word list is full. */
    LIMIT_REACHED,
}

/** The three lists the dictionary page switches between. */
enum class DictionaryFilter { LEARNED, ADDED, BLOCKED }

enum class DictionarySort {
    MOST_USED,
    RECENT,

    /** Marks ignored first, so `ăn` sits beside `an` rather than after `z`. */
    ALPHABETICAL,
}

data class DictionaryWord(
    val text: String,
    val uses: Int,
    /** Comparable between words of one store; not a time. */
    val lastUsed: Long,
    val origin: DictionaryOrigin,
)

data class DictionaryQuery(
    val filter: DictionaryFilter = DictionaryFilter.LEARNED,
    val sort: DictionarySort = DictionarySort.MOST_USED,
    /** Matched anywhere in a word, marks and case ignored. Empty matches all. */
    val search: String = "",
)

/** One load: the words of the chosen list, and how many each list holds regardless of the search. */
data class DictionaryPage(
    val words: List<DictionaryWord> = emptyList(),
    val counts: Map<DictionaryFilter, Int> = emptyMap(),
) {
    fun count(filter: DictionaryFilter): Int = counts[filter] ?: 0
}

object DictionaryLimits {
    /** The longest word the store holds, in code points; the engine's own token limit. */
    const val MAX_WORD_LENGTH = 32
}
