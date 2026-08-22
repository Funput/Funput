package app.funput.funput.ime.editing

import app.funput.funput.ime.editing.composition.CompositionBoundary

/**
 * Last committed word plus trailing separators, walked back on each Backspace.
 *
 * Fresh [wordBeforeCursor] reads are enough on well-behaved editors. Committed-mode
 * hosts often withhold or stale-read surrounding text, so a refused word (`dungh`)
 * has to stay in this suffix until a later delete makes it adoptable (`dung`).
 */
internal class CommittedWordTail {
    private val tail = StringBuilder()
    private var wordStart = 0

    fun clear() {
        tail.setLength(0)
        wordStart = 0
    }

    fun record(committed: String) {
        tail.append(committed)
        trim()
    }

    fun backspace(): String? {
        if (tail.isEmpty()) return null
        val last = Character.codePointBefore(tail, tail.length)
        tail.setLength(tail.length - Character.charCount(last))
        val word = tail.wordAtEnd() ?: return null
        wordStart = tail.length - word.length
        return word
    }

    fun resolve(adopted: Boolean) {
        if (adopted) tail.setLength(wordStart)
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
