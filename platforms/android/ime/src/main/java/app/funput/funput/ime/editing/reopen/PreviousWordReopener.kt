package app.funput.funput.ime.editing.reopen

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.recentText

/**
 * Picks the word Backspace may reopen for retoning, and never one the caret has left.
 *
 * The committed tail answers for hosts that withhold the text before the caret. It is only as
 * good as the caret it ends at, so [TailCaretAnchor] drops it once the user moves the caret, and
 * whenever the editor does show its text, that text decides: reopening deletes the word's length
 * before the caret, so the word has to be what actually sits there.
 */
internal class PreviousWordReopener(private val adopt: (String) -> Boolean) {
    private val tail = CommittedWordTail()
    private val anchor = TailCaretAnchor()

    /** The live composition [composed] was committed as [written]. */
    fun committed(composed: String, written: String) {
        tail.record(written)
        anchor.committed(composed.length, written.length)
    }

    /** [replacement] rewrote [previous], which was committed right before the caret. */
    fun rewrote(previous: String, replacement: String) {
        tail.clear()
        tail.record(replacement)
        anchor.edited(previous.length, replacement.length)
    }

    /** [owned] means the live composition claims this caret, which makes it ours. */
    fun caretReported(start: Int, end: Int, composingStart: Int, owned: Boolean) {
        if (owned) anchor.settle(end, composingStart) else if (!anchor.accepts(start, end)) tail.clear()
    }

    /**
     * Called right after Backspace deleted outside any selection; [before] is the text the editor
     * showed before that delete. Returns the word the engine adopted, or null when nothing before
     * the caret can be reopened.
     */
    fun wordToReopen(connection: InputConnection, before: CharSequence?): String? {
        val reads = BackspaceReads(before, after = connection.recentText())
        val step = tail.backspace()
        if (step == null) anchor.forget() else anchor.edited(replaced = step.erased.length, written = 0)
        val remembered = step?.word?.takeIf { reads.confirms(it, step.erased) }
        val contradicted = step?.word != null && remembered == null
        val word = listOfNotNull(remembered, reads.editorWord).distinct().firstOrNull(adopt)
        when {
            // The reopened composition holds the word now; the tail keeps what came before it.
            word != null && word == remembered -> tail.adoptWord()
            // The editor's own word, or text that contradicts the tail: it no longer describes it.
            word != null || contradicted -> tail.clear()
        }
        return word
    }

    fun clear() {
        tail.clear()
        anchor.forget()
    }
}
