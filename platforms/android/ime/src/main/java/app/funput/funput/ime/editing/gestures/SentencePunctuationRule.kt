package app.funput.funput.ime.editing.gestures

/**
 * Decides whether a second space should become a full stop.
 *
 * Mirrors the system keyboard: the substitution only applies when the caret sits behind
 * a single space that closes a word, so `"xin chao "` qualifies while `"xin chao. "`,
 * `"(  "` and the start of a document do not. A newline or tab before the caret is not a
 * word ending anyone means to punctuate, which the letter-or-number test also rejects.
 */
internal object SentencePunctuationRule {
    fun appliesTo(contextBeforeInput: String?): Boolean {
        val context = contextBeforeInput ?: return false
        if (!context.endsWith(" ")) return false
        val preceding = context.dropLast(1).lastOrNull() ?: return false
        return preceding.isLetterOrDigit()
    }
}
