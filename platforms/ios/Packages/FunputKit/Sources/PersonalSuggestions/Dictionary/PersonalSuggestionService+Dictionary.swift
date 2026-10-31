import KeyboardRenderer
import PersonalDictionary

extension PersonalSuggestionService {
    /// Applies an edit chosen on a suggestion, then asks again so the bar stops
    /// offering what was just blocked or forgotten.
    ///
    /// The engine stores words lowercase and the bar shows them in the user's case,
    /// so the edit goes out under the stored spelling. A candidate from a bar that
    /// has moved on since it was drawn is ignored, as a tap on it would be.
    public func edit(_ candidate: KeyboardSuggestionCandidate, _ kind: DictionaryEditKind) {
        guard let word = storedWord(for: candidate) else { return }
        ensureWorker()?.edit(kind, word: word)
        requery()
    }

    func storedWord(for candidate: KeyboardSuggestionCandidate) -> String? {
        guard candidate.kind == .suggestion,
              candidate.generation == generation,
              let index = visibleCandidates.firstIndex(of: candidate),
              received.indices.contains(index)
        else { return nil }
        return received[index].text
    }
}
