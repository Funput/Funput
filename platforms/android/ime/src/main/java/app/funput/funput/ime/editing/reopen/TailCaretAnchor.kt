package app.funput.funput.ime.editing.reopen

import app.funput.funput.ime.editing.composition.ExpectedCaret

/**
 * Tells our own caret moves from the user's while no word is being composed.
 *
 * The committed tail describes the text right before the caret only while the caret stays where
 * our writes left it. Hosts echo each write late, sometimes in several steps, so every caret an
 * in-flight write passes through is still ours; a report outside them, or any report once they
 * have landed that is not the confirmed caret, is the user moving the caret.
 */
internal class TailCaretAnchor {
    private val caret = ExpectedCaret()
    /** Where the live composing region starts, as the editor last reported it. */
    private var composingStart: Int? = null

    /**
     * The live composition was committed as [written]. Measured from where its region starts:
     * echoes of the last keystrokes may still be on their way, leaving the confirmed caret behind,
     * while the region's start stays put for the whole word.
     */
    fun committed(composed: Int, written: Int) {
        caret.expect(composed, written, from = composingStart?.plus(composed))
        composingStart = null
    }

    /** A write of ours turned the [replaced] characters right before the caret into [written]. */
    fun edited(replaced: Int, written: Int) {
        caret.expect(replaced, written)
    }

    /** The composition owns [end], so it is ours; [composingStart] is -1 without a region. */
    fun settle(end: Int, composingStart: Int) {
        caret.confirm(end)
        this.composingStart = composingStart.takeIf { it >= 0 }
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
        val pending = caret.target
        val ours = when {
            pending != null -> caret.span?.contains(end) == true
            caret.confirmed != null -> end == caret.confirmed
            else -> true
        }
        if (!ours || pending == null || end == pending) caret.confirm(end)
        return ours
    }

    /** Drops every position: nothing is known about where the caret is. */
    fun forget() {
        caret.forget()
        composingStart = null
    }
}
