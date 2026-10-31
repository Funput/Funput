package app.funput.funput.ime.suggestions.dictionary

/**
 * The `int` codes the native dictionary calls trade in. The values are fixed by
 * crates/funput-jni/src/suggestion/dictionary/codes.rs and match the C ABI's.
 */
internal object DictionaryCodes {
    /** Uses, last used and origin: how many numbers each word adds to the details array. */
    const val DETAILS_PER_WORD = 3

    /** The engine's "all" filter, for counting without a list in mind. */
    const val FILTER_ALL = 0

    fun of(kind: DictionaryEditKind): Int = kind.ordinal

    /** The engine's own filter; the blocked list has a call of its own. */
    fun of(filter: DictionaryFilter): Int = when (filter) {
        DictionaryFilter.LEARNED -> 1
        DictionaryFilter.ADDED -> 2
        DictionaryFilter.BLOCKED -> FILTER_ALL
    }

    fun of(sort: DictionarySort): Int = sort.ordinal

    /** An unknown code is treated as a refusal rather than guessed at. */
    fun outcome(code: Int): DictionaryEditOutcome =
        DictionaryEditOutcome.entries.getOrNull(code) ?: DictionaryEditOutcome.INVALID

    fun origin(code: Long): DictionaryOrigin = when (code) {
        1L -> DictionaryOrigin.ADDED
        else -> DictionaryOrigin.LEARNED
    }

    /** The two parallel arrays of a listing as words; empty when either is missing or they disagree. */
    fun words(words: Array<String>?, details: LongArray?): List<DictionaryWord> {
        if (words == null || details == null || details.size != words.size * DETAILS_PER_WORD) return emptyList()
        return words.mapIndexed { index, text ->
            val at = index * DETAILS_PER_WORD
            DictionaryWord(
                text = text,
                uses = details[at].coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
                lastUsed = details[at + 1],
                origin = origin(details[at + 2]),
            )
        }
    }
}
