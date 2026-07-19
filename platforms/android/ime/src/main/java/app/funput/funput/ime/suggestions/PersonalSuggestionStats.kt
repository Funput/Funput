package app.funput.funput.ime.suggestions

internal data class PersonalSuggestionStats(
    val words: Long,
    val promotedWords: Long,
    val exactNodes: Long,
    val foldedNodes: Long,
    val pendingMutations: Long,
    val journalBytes: Long,
    val estimatedHeapBytes: Long,
    val lastSnapshotBytes: Long,
) {
    companion object {
        val Empty = PersonalSuggestionStats(0, 0, 0, 0, 0, 0, 0, 0)

        fun decode(values: LongArray?): PersonalSuggestionStats {
            if (values == null || values.size != 8) return Empty
            return PersonalSuggestionStats(
                values[0], values[1], values[2], values[3],
                values[4], values[5], values[6], values[7],
            )
        }
    }
}
