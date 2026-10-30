package app.funput.funput.ime.editing.reopen

import app.funput.funput.ime.editing.composition.CompositionBoundary
import app.funput.funput.ime.editing.wordAtEnd

/** What one Backspace erased from the tail, and the word that now ends it (null after a separator). */
internal data class TailStep(val erased: String, val word: String?)

/**
 * Last committed word plus trailing separators, walked back on each Backspace.
 *
 * Hosts that withhold or stale-read the text before the caret give no fresh word to reopen,
 * so a refused word (`dungh`) has to stay in this suffix until a later delete makes it
 * adoptable (`dung`). [PreviousWordReopener] decides when the suffix can be trusted.
 */
internal class CommittedWordTail {
    private val tail = StringBuilder()
    /** Where the word the last [backspace] offered starts, until it is adopted or passed over. */
    private var offeredStart: Int? = null

    fun clear() {
        tail.setLength(0)
        offeredStart = null
    }

    fun record(committed: String) {
        offeredStart = null
        tail.append(committed)
        trim()
    }

    /** Erases the last code point, as Backspace did in the editor; null when nothing is left. */
    fun backspace(): TailStep? {
        if (tail.isEmpty()) return null
        val last = Character.codePointBefore(tail, tail.length)
        val erased = tail.substring(tail.length - Character.charCount(last))
        tail.setLength(tail.length - erased.length)
        val word = tail.wordAtEnd()
        offeredStart = word?.let { tail.length - it.length }
        return TailStep(erased, word)
    }

    /** Drops the word the last [backspace] offered: it now lives in the reopened composition. */
    fun adoptWord() {
        offeredStart?.let(tail::setLength)
        offeredStart = null
    }

    private fun trim() {
        if (tail.length <= CAPACITY) return
        val floor = tail.length - CAPACITY
        var index = 0
        while (index < tail.length) {
            val codePoint = Character.codePointAt(tail, index)
            val width = Character.charCount(codePoint)
            if (index >= floor && CompositionBoundary.isBoundary(codePoint)) {
                tail.delete(0, index + width)
                return
            }
            index += width
        }
        tail.setLength(0)
    }

    private companion object {
        const val CAPACITY = 64
    }
}
