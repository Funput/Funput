import Foundation
import PersonalDictionary
import PersonalSuggestions

extension PersonalSuggestionWorker {
    /// Applies an edit chosen on the suggestion bar and makes it durable at once:
    /// it is a deliberate choice, not a keystroke, so it does not wait for the
    /// learn path's batching.
    func edit(_ kind: DictionaryEditKind, word: String) {
        queue.async { [weak self] in
            guard let engine = self?.engine else { return }
            guard engine.applyEdit(kind, word: word) == .applied else { return }
            engine.flush()
        }
    }
}
