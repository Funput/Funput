package app.funput.funput.ime.editing

internal class AuthoredTokenTracker {
    private var prefix = ""
    private var completedToken: String? = null

    /**
     * The words the next one follows. They live in the tracker because the tracker is
     * the only thing that sees every edit — which separator ended a word, when the
     * caret left, when the prefix was abandoned. Everything downstream sees the
     * result, not the reason.
     */
    private val context = SuggestionContext()
    private var directOverflow = false

    fun update(composingText: String, completed: String?, completedOnSpace: Boolean) {
        directOverflow = false
        prefix = composingText.takeIf(::isValidToken).orEmpty()
        completedToken = completed?.takeIf { isValidToken(it) && scalarCount(it) >= MinimumTokenLength }
        if (completed != null) context.follow(completedToken.takeIf { completedOnSpace })
    }

    fun accepted(candidate: String) {
        directOverflow = false
        prefix = ""
        completedToken = candidate.takeIf(::isValidToken)
        // Accepting a candidate writes the word and a space after it.
        context.follow(completedToken)
    }

    fun input(text: String) {
        var index = 0
        while (index < text.length) {
            val codePoint = text.codePointAt(index)
            if (isTokenScalar(codePoint)) {
                if (!directOverflow && scalarCount(prefix) < MaximumTokenLength) {
                    prefix += String(Character.toChars(codePoint))
                } else {
                    prefix = ""
                    completedToken = null
                    directOverflow = true
                }
            } else {
                completedToken = prefix.takeIf {
                    !directOverflow && scalarCount(it) >= MinimumTokenLength
                }
                // A space continues the sentence, so the word just finished is
                // what the next one follows. Anything else ends it, and the two
                // words either side were never adjacent.
                context.follow(completedToken.takeIf { codePoint == ' '.code })
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
            context.follow(null)
            return
        }
        prefix = prefix.dropLast(Character.charCount(prefix.codePointBefore(prefix.length)))
        completedToken = null
    }

    /**
     * The caret moved without a key, and [text] is what the editor now reports before
     * it. With no word in progress, the words to ask about are read back from there.
     */
    fun reconcileContext(text: CharSequence) {
        if (prefix.isEmpty() && !directOverflow) context.reconcile(text)
    }

    fun consume(): AuthoredSuggestionUpdate =
        AuthoredSuggestionUpdate(prefix, completedToken, context.word, context.older, context.borrowed)
            .also { completedToken = null }

    fun currentPrefix(): String = prefix

    fun reset() {
        directOverflow = false
        prefix = ""
        completedToken = null
        context.follow(null)
    }

    private fun isValidToken(text: String): Boolean {
        if (text.isEmpty()) return false
        var index = 0
        var count = 0
        while (index < text.length && count <= MaximumTokenLength) {
            val codePoint = text.codePointAt(index)
            if (!isTokenScalar(codePoint)) return false
            index += Character.charCount(codePoint)
            count += 1
        }
        return count in 1..MaximumTokenLength
    }

    private fun scalarCount(text: String): Int = text.codePointCount(0, text.length)
}
