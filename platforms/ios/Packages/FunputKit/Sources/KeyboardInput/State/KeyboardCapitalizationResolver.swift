enum KeyboardCapitalizationResolver {
    static func shouldUppercase(
        mode: KeyboardAutocapitalizationMode,
        contextBeforeInput: String?
    ) -> Bool? {
        guard let contextBeforeInput else { return nil }
        return switch mode {
        case .none:
            false
        case .allCharacters:
            true
        case .words:
            contextBeforeInput.last.map { !$0.isLetter && !$0.isNumber } ?? true
        case .sentences:
            startsSentence(contextBeforeInput)
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
