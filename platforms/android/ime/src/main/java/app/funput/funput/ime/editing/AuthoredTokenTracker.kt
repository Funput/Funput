package app.funput.funput.ime.editing

internal data class AuthoredSuggestionUpdate(
    val prefix: String,
    val completedToken: String?,
) {
    companion object {
        val Empty = AuthoredSuggestionUpdate("", null)
    }
}

internal class AuthoredTokenTracker {
    private var prefix = ""
    private var completedToken: String? = null

    fun update(composingText: String, completed: String?) {
        prefix = composingText.takeIf(::isValidToken).orEmpty()
        completedToken = completed?.takeIf { isValidToken(it) && scalarCount(it) >= MinimumLength }
    }

    fun accepted(candidate: String) {
        prefix = ""
        completedToken = candidate.takeIf(::isValidToken)
    }

    fun consume(): AuthoredSuggestionUpdate = AuthoredSuggestionUpdate(prefix, completedToken).also {
        completedToken = null
    }

    fun reset() {
        prefix = ""
        completedToken = null
    }

    private fun isValidToken(text: String): Boolean {
        if (text.isEmpty()) return false
        var index = 0
        var count = 0
        while (index < text.length && count <= MaximumLength) {
            val codePoint = text.codePointAt(index)
            if (!Character.isLetter(codePoint) && !isCombiningMark(codePoint)) return false
            index += Character.charCount(codePoint)
            count += 1
        }
        return count in 1..MaximumLength
    }

    private fun scalarCount(text: String): Int = text.codePointCount(0, text.length)

    private fun isCombiningMark(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.NON_SPACING_MARK.toInt(),
        Character.COMBINING_SPACING_MARK.toInt(),
        Character.ENCLOSING_MARK.toInt(),
        -> true
        else -> false
    }

    private companion object {
        const val MinimumLength = 2
        const val MaximumLength = 32
    }
}
