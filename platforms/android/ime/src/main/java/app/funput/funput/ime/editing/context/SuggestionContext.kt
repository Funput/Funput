package app.funput.funput.ime.editing

/**
 * The words before the caret that the suggestion engine may read: the last one, and
 * the one before it when the two stand in a row.
 *
 * They come from two places. A word typed through Funput is authored: the tracker saw
 * it end with a space, so the next word may be learned as following it. A word read
 * back from the document after the caret moved is borrowed: good enough to ask the
 * engine about, never to learn from.
 */
internal class SuggestionContext {
    /** The word the next one follows, or null when nothing can vouch for one. */
    var word: String? = null
        private set

    /** The word [word] stands after. */
    var older: String? = null
        private set

    var borrowed = false
        private set

    /** [next] was just typed and is now the word before the caret; null ends the run. */
    fun follow(next: String?) {
        older = next?.let { word }
        word = next
        borrowed = false
    }

    /**
     * Brings the context in line with [text], what the editor reports before the caret.
     *
     * A word the document still ends with is left as it was, and an authored one keeps
     * its right to be learned from: the editor reports a selection change after
     * Funput's own space too, and that must not turn every typed word into a borrowed one.
     */
    fun reconcile(text: CharSequence) {
        val found = text.wordsBeforeCaret()
        if (found?.last.equals(word, ignoreCase = true)) return
        word = found?.last
        older = found?.older
        borrowed = found != null
    }
}
