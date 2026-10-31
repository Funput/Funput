#if os(iOS) && canImport(FunputCore)
import FunputCore
import PersonalDictionary

/// The keyboard's side of the dictionary: it owns the store, so it edits directly
/// and folds in what the app posted.
extension PersonalSuggestionEngine {
    /// Durable on the next `flush`, like anything learned.
    public func applyEdit(_ kind: DictionaryEditKind, word: String) -> DictionaryEditOutcome {
        let codepoints = DictionaryFFI.codepoints(word)
        let code = codepoints.withUnsafeBufferPointer {
            funput_suggestion_apply_edit(handle, kind.code, $0.baseAddress, UInt($0.count))
        }
        return DictionaryEditOutcome(code: code)
    }

    /// Applies the edits the app posted, journals them, and only then removes them
    /// from the inbox. How many were read, or `nil` when the inbox could not be read
    /// or the journal not written; the edits then wait for the next call.
    @discardableResult
    public func applyPendingEdits() -> Int? {
        let count = funput_suggestion_apply_pending_edits(handle)
        return count < 0 ? nil : Int(count)
    }
}
#endif
