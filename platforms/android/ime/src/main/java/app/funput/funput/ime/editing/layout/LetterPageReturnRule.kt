package app.funput.funput.ime.editing.layout

/**
 * Decides whether a space typed on the symbol panel should bring back the letters.
 *
 * Punctuation that ends a sentence or a clause, or closes a bracket or a quote, means the
 * next thing typed is a word, so `"Chào bạn! "` returns while `"10 "` and `"8:30 "` stay
 * on the panel where the rest of the number is. The set matches iOS; widen [Triggers] to
 * widen the rule.
 */
internal object LetterPageReturnRule {
    val Triggers: Set<Char> = setOf(
        '.', ',', '!', '?', ';', ':', '…',
        ')', ']', '}', '"', '\'', '»', '”', '’',
    )

    /** [textBeforeCursor] is read before the space is committed. */
    fun appliesTo(textBeforeCursor: CharSequence?): Boolean =
        textBeforeCursor?.lastOrNull() in Triggers
}
