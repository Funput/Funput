package app.funput.funput.ime.editing

/** The shortest word the trackers learn, in scalars, and the shortest prefix they ask about. */
internal const val MinimumTokenLength = 2

/** The longest word the trackers learn, in scalars. */
internal const val MaximumTokenLength = 32

/** How much text before the caret to read: room for two of the longest words and their spaces. */
internal const val ContextLookback = 2 * (MaximumTokenLength + 1)

/** The word a caret sits after, and the word before that one when a single space parts them. */
internal data class DocumentWords(val last: String, val older: String?)

/**
 * The words a caret sits after, when this text (what an editor reports before the
 * caret, at most [ContextLookback] characters of it) ends with a word and one space.
 *
 * Any other ending has no word to follow: the caret is inside a word, after
 * punctuation, after two spaces, or at the start of a line.
 */
internal fun CharSequence.wordsBeforeCaret(): DocumentWords? {
    val space = length - 1
    if (space < 0 || this[space] != ' ') return null
    val last = tokenEndingAt(space) ?: return null
    val start = space - last.length
    val older = if (start > 0 && this[start - 1] == ' ') tokenEndingAt(start - 1) else null
    return DocumentWords(last, older)
}

/** The whole word that ends just before [end], when it is one the trackers would learn. */
private fun CharSequence.tokenEndingAt(end: Int): String? {
    var start = end
    while (start > 0) {
        val codePoint = Character.codePointBefore(this, start)
        if (!isTokenScalar(codePoint)) break
        start -= Character.charCount(codePoint)
    }
    // A word that reaches the start of a full window may have been cut by it.
    if (start == 0 && length >= ContextLookback) return null
    val token = subSequence(start, end).toString()
    return token.takeIf { it.codePointCount(0, it.length) in MinimumTokenLength..MaximumTokenLength }
}

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
internal fun isTokenScalar(codePoint: Int) = Character.isLetter(codePoint) || isCombiningMark(codePoint)

private fun isCombiningMark(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
    Character.NON_SPACING_MARK.toInt(),
    Character.COMBINING_SPACING_MARK.toInt(),
    Character.ENCLOSING_MARK.toInt(),
    -> true
    else -> false
}
