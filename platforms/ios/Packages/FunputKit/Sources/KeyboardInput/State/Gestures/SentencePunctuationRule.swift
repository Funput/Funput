/// Decides whether a second space should become a full stop.
///
/// Mirrors the system keyboard: the substitution only applies when the caret sits behind
/// a single space that closes a word, so `"xin chao "` qualifies while `"xin chao. "`,
/// `"(  "` and the start of a document do not. A newline or tab before the caret is not a
/// word ending anyone means to punctuate, which the letter-or-number test also rejects.
enum SentencePunctuationRule {
    static func appliesTo(contextBeforeInput: String?) -> Bool {
        guard let context = contextBeforeInput, context.hasSuffix(" ") else { return false }
        guard let preceding = context.dropLast().last else { return false }
        return preceding.isLetter || preceding.isNumber
    }
}
