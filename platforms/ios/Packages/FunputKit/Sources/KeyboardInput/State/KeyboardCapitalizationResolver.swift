#if os(iOS) && canImport(FunputCore)
import FunputEngine

enum KeyboardCapitalizationResolver {
    /// - Parameter contextBeforeInput: `nil` is how `UITextDocumentProxy` reports that
    ///   nothing precedes the caret, so it means the same thing as `""`: the start of
    ///   the document, which is the start of a sentence and of a word. Reading it as
    ///   "unknown" and leaving the shift state untouched is what left the very first
    ///   character lowercase — measured in an empty field where the system keyboard
    ///   capitalized and Funput did not.
    ///
    /// Only the two modes that depend on surrounding text reach ``FunputSentence``;
    /// the other two are answers about the field, not about the caret.
    static func shouldUppercase(
        mode: KeyboardAutocapitalizationMode,
        contextBeforeInput: String?
    ) -> Bool {
        let context = contextBeforeInput ?? ""
        return switch mode {
        case .none: false
        case .allCharacters: true
        case .words: FunputSentence.startsWord(context)
        case .sentences: FunputSentence.startsSentence(context)
        }
    }
}
#endif
