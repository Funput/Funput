package app.funput.funput.ime.editing

internal data class AuthoredSuggestionUpdate(
    val prefix: String,
    val completedToken: String?,
    /** The word [prefix] is being typed after, when one can be vouched for. */
    val context: String? = null,
) {
    companion object {
        val Empty = AuthoredSuggestionUpdate("", null)
    }
}

internal class AuthoredTokenTracker {
    private var prefix = ""
    private var completedToken: String? = null

    /**
     * The word the next one will be recorded as following, or null when nothing
     * here can vouch for what came before.
     *
     * It lives in the tracker because the tracker is the only thing that sees
     * every edit — which separator ended a word, when the caret left, when the
     * prefix was abandoned. Everything downstream sees the result, not the reason.
     */
    private var context: String? = null
    private var directOverflow = false

    fun update(composingText: String, completed: String?, completedOnSpace: Boolean) {
        directOverflow = false
        prefix = composingText.takeIf(::isValidToken).orEmpty()
        completedToken = completed?.takeIf { isValidToken(it) && scalarCount(it) >= MinimumLength }
        if (completed != null) context = completedToken.takeIf { completedOnSpace }
    }

    fun accepted(candidate: String) {
        directOverflow = false
        prefix = ""
        completedToken = candidate.takeIf(::isValidToken)
        // Accepting a candidate writes the word and a space after it.
        context = completedToken
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
                // A space continues the sentence, so the word just finished is
                // what the next one follows. Anything else ends it, and the two
                // words either side were never adjacent.
                context = completedToken.takeIf { codePoint == ' '.code }
                prefix = ""
                directOverflow = false
            }
            index += Character.charCount(codePoint)
        }
    }

    fun backspace() {
        if (directOverflow || prefix.isEmpty()) {
            // Nothing left of this word to delete, so the caret has crossed back
            // over a boundary and what precedes it is no longer known.
            context = null
            return
        }
        prefix = prefix.dropLast(Character.charCount(prefix.codePointBefore(prefix.length)))
        completedToken = null
    }

    fun consume(): AuthoredSuggestionUpdate =
        AuthoredSuggestionUpdate(prefix, completedToken, context).also { completedToken = null }

    fun currentPrefix(): String = prefix

    fun reset() {
        directOverflow = false
        prefix = ""
        completedToken = null
        context = null
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

    /**
     * Letters and their marks, and nothing else — a digit ends a word here as it
     * does on iOS.
     *
     * The two trackers have to agree, or the same typing teaches two different
     * lexicons. Letters is the rule that agrees with what is being learned:
     * Vietnamese syllables contain no digits, so a run of them is never a word,
     * and treating it as one spends a slot out of five thousand on a date or a
     * price — and writes a phone number into the store on disk.
     *
     * It only comes up with the number row turned on. Otherwise digits are typed
     * from the symbols panel, where nothing is learned at all.
     */
    private fun isTokenScalar(codePoint: Int) =
        Character.isLetter(codePoint) || isCombiningMark(codePoint)

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
