import KeyboardLayout
import KeyboardRenderer

extension PersonalSuggestionService {
    /// The stored words in the case the user is typing in. Derived rather than kept, so
    /// the list on screen and the list `acceptance(for:)` matches can never disagree —
    /// a tap is refused when they do.
    var visibleCandidates: [KeyboardSuggestionCandidate] {
        let style = SuggestionCaseStyle.resolve(prefix: prefix, shift: shift)
        return received.map {
            KeyboardSuggestionCandidate(text: style.apply(to: $0.text), generation: generation)
        }
    }

    /// What accepting `candidate` replaces and inserts, or `nil` when the bar has
    /// moved on since it was drawn.
    public func acceptance(
        for candidate: KeyboardSuggestionCandidate
    ) -> (String, String)? {
        // An empty prefix is the ordinary shape of accepting a prediction: it
        // replaces nothing and inserts a word.
        guard candidate.generation == generation,
              visibleCandidates.contains(candidate)
        else { return nil }
        return (prefix, candidate.text)
    }

    /// Shift moved while a list was already up.
    ///
    /// Nothing is asked of the engine: these are the same words, only their shape
    /// changed. Going through `update` instead would be swallowed by its dedupe, which
    /// compares the prefix and knows nothing about case.
    public func recase(shift: ShiftState) {
        guard shift != self.shift else { return }
        self.shift = shift
        guard !received.isEmpty else { return }
        publish(visibleCandidates)
    }
}
