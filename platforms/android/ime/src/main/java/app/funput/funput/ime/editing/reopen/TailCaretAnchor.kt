package app.funput.funput.ime.editing.reopen

/**
 * Tells our own caret moves from the user's while no word is being composed.
 *
 * The committed tail describes the text right before the caret only while the caret stays where
 * our writes left it. Hosts echo each write through `onUpdateSelection`, sometimes in several
 * steps and late, so a write opens a window of positions that are still ours: the caret before
 * the write, the caret between its delete and its commit, and the caret after it. A report
 * outside that window, or any report once the window has settled that is not the settled caret,
 * is the user moving the caret.
 */
internal class TailCaretAnchor {
    private var settled: Int? = null
    private var target: Int? = null
    private var window: IntRange? = null

    /** Records a write of ours that turns [replaced] characters before the caret into [written]. */
    fun expect(replaced: Int, written: Int) {
        val base = target ?: settled ?: return forget()
        val afterDelete = base - replaced
        val next = afterDelete + written
        val previous = window ?: base..base
        window = minOf(previous.first, afterDelete)..maxOf(previous.last, base, next)
        target = next
    }

    /** Takes [end] as the caret: the composition owns it, so it is ours by construction. */
    fun settle(end: Int) {
        settled = end
        target = null
        window = null
    }

    /**
     * Whether a caret report at [start]..[end] can still be the echo of our own writes.
     * A rejected caret becomes the new reference, so later writes are measured from it; a
     * selection leaves no reference until the next report.
     */
    fun accepts(start: Int, end: Int): Boolean {
        if (start != end) {
            forget()
            return false
        }
        val pending = target
        val ours = when {
            pending != null -> window?.contains(end) == true
            settled != null -> end == settled
            else -> true
        }
        if (!ours || pending == null || end == pending) settle(end)
        return ours
    }

    /** Drops every position: nothing is known about where the caret is. */
    fun forget() {
        settled = null
        target = null
        window = null
    }
}
