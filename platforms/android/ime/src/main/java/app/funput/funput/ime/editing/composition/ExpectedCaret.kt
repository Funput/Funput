package app.funput.funput.ime.editing.composition

/**
 * The caret our own writes should leave, measured from the last caret the editor confirmed.
 *
 * Hosts echo each write through `onUpdateSelection` after the fact, so a write is expected before
 * it is confirmed. [span] covers every caret the writes still in flight pass through: the caret
 * before a write, the caret between its delete and its commit, and the caret after it.
 */
internal class ExpectedCaret {
    var confirmed: Int? = null
        private set
    var target: Int? = null
        private set
    var span: IntRange? = null
        private set

    /**
     * Records a write that turns the [replaced] characters before [from] into [written] ones;
     * [from] defaults to where the previous writes leave the caret.
     * Returns the caret between the write's delete and its commit, or null when no caret is known.
     */
    fun expect(replaced: Int, written: Int, from: Int? = null): Int? {
        val base = from ?: target ?: confirmed
        if (base == null) {
            forget()
            return null
        }
        val afterDelete = base - replaced
        val next = afterDelete + written
        val previous = span ?: base..base
        span = minOf(previous.first, afterDelete)..maxOf(previous.last, base, next)
        target = next
        return afterDelete
    }

    /** The editor reported [caret] and it is ours: later writes are measured from it. */
    fun confirm(caret: Int) {
        confirmed = caret
        target = null
        span = null
    }

    fun forget() {
        confirmed = null
        target = null
        span = null
    }
}
