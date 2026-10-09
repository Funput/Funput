/// Decides whether a space typed on a symbol page should bring back the letters.
///
/// Punctuation that ends a sentence or a clause, or closes a bracket or a quote, means the
/// next thing typed is a word, so `"Chào bạn! "` returns while `"10 "` and `"8:30 "` stay
/// on the page where the rest of the number is. Extend ``triggers`` to widen the rule.
enum LetterPageReturnRule {
    static let triggers: Set<Character> = [
        ".", ",", "!", "?", ";", ":", "…",
        ")", "]", "}", "\"", "'", "»", "”", "’",
    ]

    /// `contextBeforeInput` is the text before the caret, before the space is inserted.
    static func appliesTo(contextBeforeInput: String?) -> Bool {
        guard let last = contextBeforeInput?.last else { return false }
        return triggers.contains(last)
    }
}
