package app.funput.funput.ime.editing.reopen

import android.view.inputmethod.InputConnection
import app.funput.funput.ime.editing.recentText
import app.funput.funput.ime.editing.wordAtEnd

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

    /** [written] replaced [replaced], the composed text right before the caret. */
    fun recorded(replaced: String, written: String) {
        tail.record(written)
        anchor.expect(replaced.length, written.length)
    }

    /** [replacement] rewrote [previous], which was committed right before the caret. */
    fun rewrote(previous: String, replacement: String) {
        tail.clear()
        recorded(previous, replacement)
    }

    /** [owned] means the live composition claims this caret, which makes it ours. */
    fun caretReported(start: Int, end: Int, owned: Boolean) {
        if (owned) anchor.settle(end) else if (!anchor.accepts(start, end)) tail.clear()
    }

    /**
     * Called right after Backspace deleted one character outside any composition.
     * Returns the word the engine adopted, or null when nothing before the caret can be reopened.
     */
    fun wordToReopen(connection: InputConnection): String? {
        anchor.expect(replaced = 1, written = 0)
        val visible = connection.recentText()
        val remembered = tail.backspace()
        val trusted = remembered?.takeIf { visible.isNullOrEmpty() || visible.endsWith(it) }
        val word = listOfNotNull(trusted, visible?.wordAtEnd()).distinct().firstOrNull(adopt)
        when {
            word != null && word == trusted -> tail.resolve(true)
            word != null || remembered != trusted -> tail.clear()
        }
        return word
    }

    fun clear() {
        tail.clear()
        anchor.forget()
    }
}
