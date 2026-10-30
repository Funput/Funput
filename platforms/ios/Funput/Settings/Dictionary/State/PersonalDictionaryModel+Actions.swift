import Foundation
import PersonalDictionary

extension PersonalDictionaryModel {
    /// Applies one edit and, when it changed anything, reads the lists again.
    ///
    /// The engine decides whether an edit applies, under the rules the keyboard will
    /// apply it by, so its answer is returned as is for the add sheet to explain.
    /// `nil` means the store could not be reached; `actionError` then says so.
    @discardableResult
    func apply(_ kind: DictionaryEditKind, to word: String) async -> DictionaryEditOutcome? {
        do {
            let outcome = try await store.apply(kind, word: word)
            if outcome == .applied { await reload() }
            return outcome
        } catch {
            actionError = error.localizedDescription
            return nil
        }
    }

    func block(_ word: DictionaryWord) async {
        await apply(.block, to: word.text)
    }

    func unblock(_ word: DictionaryWord) async {
        await apply(.unblock, to: word.text)
    }

    func forget(_ word: DictionaryWord) async {
        await apply(.forget, to: word.text)
    }
}
