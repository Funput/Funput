package app.funput.funput.ime.editing.gestures

import app.funput.funput.ime.editing.composition.CompositionBoundary

/**
 * The number of UTF-16 units a word-delete should remove, or null when there is
 * nothing measurable behind the caret.
 *
 * Trailing whitespace goes first, so a swipe after `"xin chao "` takes the word too
 * rather than only the space the caret visually sits behind. What follows is either a
 * word or a run of punctuation — `"a!!!"` loses all three marks, not one.
 */
internal object KeyboardWordDeletion {
    fun spanBeforeCursor(context: String?): Int? {
        if (context.isNullOrEmpty()) return null
        val spaces = trailingSpaces(context)
        val trimmedEnd = context.length - spaces
        if (trimmedEnd == 0) return spaces.takeIf { it > 0 }
        val last = context.codePointBefore(trimmedEnd)
        val run = if (CompositionBoundary.isBoundary(last)) {
            punctuationRun(context, trimmedEnd)
        } else {
            wordRun(context, trimmedEnd)
        }
        val span = spaces + run
        return span.takeIf { it > 0 }
    }

    private fun trailingSpaces(context: String): Int {
        var index = context.length
        var count = 0
        while (index > 0) {
            val codePoint = context.codePointBefore(index)
            if (!isNonNewlineWhitespace(codePoint)) break
            val width = Character.charCount(codePoint)
            count += width
            index -= width
        }
        return count
    }

    private fun punctuationRun(context: String, end: Int): Int {
        var index = end
        var count = 0
        while (index > 0) {
            val codePoint = context.codePointBefore(index)
            if (!CompositionBoundary.isBoundary(codePoint) || Character.isWhitespace(codePoint)) break
            val width = Character.charCount(codePoint)
            count += width
            index -= width
        }
        return count
    }

    private fun wordRun(context: String, end: Int): Int {
        var index = end
        var count = 0
        while (index > 0) {
            val codePoint = context.codePointBefore(index)
            if (CompositionBoundary.isBoundary(codePoint)) break
            val width = Character.charCount(codePoint)
            count += width
            index -= width
        }
        return count
    }

    private fun isNonNewlineWhitespace(codePoint: Int): Boolean {
        if (!Character.isWhitespace(codePoint)) return false
        val type = Character.getType(codePoint)
        return type != Character.LINE_SEPARATOR.toInt() &&
            type != Character.PARAGRAPH_SEPARATOR.toInt() &&
            codePoint != '\n'.code &&
            codePoint != '\r'.code
    }
}
