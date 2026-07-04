package app.funput.funput.ui.playground

internal object PlaygroundTokenBoundary {
    fun rangeAt(text: String, cursor: Int): IntRange? {
        var start = cursor
        while (start > 0) {
            val codePoint = text.codePointBefore(start)
            if (!isTokenCodePoint(codePoint)) break
            start -= Character.charCount(codePoint)
        }

        var end = cursor
        while (end < text.length) {
            val codePoint = text.codePointAt(end)
            if (!isTokenCodePoint(codePoint)) break
            end += Character.charCount(codePoint)
        }
        return if (start < end) start until end else null
    }

    private fun isTokenCodePoint(codePoint: Int): Boolean {
        if (Character.isLetterOrDigit(codePoint)) return true
        return Character.getType(codePoint) in MarkTypes
    }

    private val MarkTypes = setOf(
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
    )
}
