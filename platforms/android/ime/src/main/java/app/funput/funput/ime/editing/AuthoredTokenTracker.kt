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
    private var directOverflow = false

    fun update(composingText: String, completed: String?) {
        directOverflow = false
        prefix = composingText.takeIf(::isValidToken).orEmpty()
        completedToken = completed?.takeIf { isValidToken(it) && scalarCount(it) >= MinimumLength }
    }

    fun accepted(candidate: String) {
        directOverflow = false
        prefix = ""
        completedToken = candidate.takeIf(::isValidToken)
    }

    fun input(text: String) {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (isTokenScalar(codePoint)) {
                if (!directOverflow && scalarCount(prefix) < MaximumLength) {
                    prefix += String(Character.toChars(codePoint))
                } else {
                    prefix = ""
                    completedToken = null
                    directOverflow = true
                }
            } else {
                completedToken = prefix.takeIf {
                    !directOverflow && scalarCount(it) >= MinimumLength
                }
                prefix = ""
                directOverflow = false
            }
            index += Character.charCount(codePoint)
        }
    }

    fun backspace() {
        if (directOverflow || prefix.isEmpty()) return
        prefix = prefix.dropLast(Character.charCount(prefix.codePointBefore(prefix.length)))
        completedToken = null
    }

    fun consume(): AuthoredSuggestionUpdate = AuthoredSuggestionUpdate(prefix, completedToken).also {
        completedToken = null
    }

    fun currentPrefix(): String = prefix

    fun reset() {
        directOverflow = false
        prefix = ""
        completedToken = null
    }

    private fun isValidToken(text: String): Boolean {
        if (text.isEmpty()) return false
        var index = 0
        var count = 0
        while (index < text.length && count <= MaximumLength) {
            val codePoint = text.codePointAt(index)
            if (!isTokenScalar(codePoint)) return false
            index += Character.charCount(codePoint)
            count += 1
        }
        return count in 1..MaximumLength
    }

    private fun scalarCount(text: String): Int = text.codePointCount(0, text.length)

    private fun isTokenScalar(codePoint: Int) =
        Character.isLetterOrDigit(codePoint) || isCombiningMark(codePoint)

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
