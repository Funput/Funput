package app.funput.funput.keyboard.interaction.backspace

/** Semantic deletion unit emitted by a single held-Backspace tick. */
internal sealed interface BackspaceRepeatStage {
    /** Deletes one character backward. */
    data object Character : BackspaceRepeatStage

    /** Deletes one word or bounded segment backward. */
    data object Word : BackspaceRepeatStage
}

/**
 * Outcome of one repeat tick: what to delete now and how long to wait before the next tick.
 */
internal data class BackspaceRepeatStep(
    val stage: BackspaceRepeatStage,
    val nextDelayMillis: Long,
) {
    init {
        require(nextDelayMillis > 0L) { "Next repeat delay must be positive" }
    }
}
