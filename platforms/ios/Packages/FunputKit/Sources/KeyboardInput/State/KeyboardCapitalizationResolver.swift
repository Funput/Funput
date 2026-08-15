enum KeyboardCapitalizationResolver {
    /// - Parameter contextBeforeInput: `nil` is how `UITextDocumentProxy` reports that
    ///   nothing precedes the caret, so it means the same thing as `""`: the start of
    ///   the document, which is the start of a sentence and of a word. Reading it as
    ///   "unknown" and leaving the shift state untouched is what left the very first
    ///   character lowercase — measured in an empty field where the system keyboard
    ///   capitalized and Funput did not.
    static func shouldUppercase(
        mode: KeyboardAutocapitalizationMode,
        contextBeforeInput: String?
    ) -> Bool {
        let context = contextBeforeInput ?? ""
        return switch mode {
        case .none:
            false
        case .allCharacters:
            true
        case .words:
            context.last.map { !$0.isLetter && !$0.isNumber } ?? true
        case .sentences:
            startsSentence(context)
        }
    }

    private static func startsSentence(_ context: String) -> Bool {
        guard let last = context.last else { return true }
        if last.isNewline { return true }
        guard last.isWhitespace else { return false }

        let preceding = context.reversed().drop(while: { $0.isWhitespace }).first
        return preceding.map { sentenceTerminators.contains($0) } ?? true
    }

    private static let sentenceTerminators: Set<Character> = [".", "!", "?", "…"]
}
