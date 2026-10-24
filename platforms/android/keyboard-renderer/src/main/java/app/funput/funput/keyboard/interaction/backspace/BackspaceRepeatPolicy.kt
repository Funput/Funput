package app.funput.funput.keyboard.interaction.backspace

/**
 * Escalation schedule for a held Backspace press.
 *
 * Starts with fine-grained character deletions that gradually speed up, then shifts into
 * whole-word/segment deletions once the hold crosses the word-phase threshold.
 */
internal fun interface BackspaceRepeatPolicy {
    /** Delay before the first repeat fires after key down. */
    val initialDelayMillis: Long
        get() = DefaultInitialDelayMillis

    /** Resolves the deletion stage and interval following [repeatIndex] (0-based). */
    fun step(repeatIndex: Int): BackspaceRepeatStep

    companion object {
        const val DefaultInitialDelayMillis = 400L
        const val WordStageStartIndex = 15
        const val InitialCharacterIntervalMillis = 50L
        const val FastCharacterIntervalMillis = 35L
        const val WordIntervalMillis = 80L

        /** Default Gboard-style two-phase acceleration curve. */
        val Default: BackspaceRepeatPolicy = BackspaceRepeatPolicy { repeatIndex ->
            require(repeatIndex >= 0) { "Repeat index must be non-negative" }
            if (repeatIndex < WordStageStartIndex) {
                BackspaceRepeatStep(
                    stage = BackspaceRepeatStage.Character,
                    nextDelayMillis = characterDelayFor(repeatIndex),
                )
            } else {
                BackspaceRepeatStep(
                    stage = BackspaceRepeatStage.Word,
                    nextDelayMillis = WordIntervalMillis,
                )
            }
        }

        private fun characterDelayFor(repeatIndex: Int): Long = when {
            repeatIndex < 5 -> InitialCharacterIntervalMillis
            repeatIndex < 10 -> 42L
            else -> FastCharacterIntervalMillis
        }
    }
}
