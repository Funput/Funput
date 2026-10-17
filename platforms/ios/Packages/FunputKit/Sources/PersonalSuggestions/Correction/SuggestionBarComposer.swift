import KeyboardInput
import KeyboardRenderer

/// Lays the bar out from its two sources: what typo correction says about the word
/// under the caret, and what the personal lexicon suggests for it.
///
/// Correction comes first. It is about the word on screen right now, whether still
/// being typed or just committed with its repair left parked, and the slot it takes
/// is the one a user's eye lands on; completions fill whatever is left of the three. Pure, so the service can recompose on either source changing, and so
/// the bar still shows a repair when personal suggestions are switched off.
public enum SuggestionBarComposer {
    public static let slotCount = 3

    public static func compose(
        correction: CorrectionPreview?,
        personal: [KeyboardSuggestionCandidate],
        generation: UInt64
    ) -> [KeyboardSuggestionCandidate] {
        var slots: [KeyboardSuggestionCandidate] = []
        switch correction {
        case let .offered(typed, candidates), let .parked(typed, candidates):
            slots.append(.init(text: typed, generation: generation, kind: .correctionTyped))
            for candidate in candidates.prefix(slotCount - 1) {
                slots.append(.init(text: candidate, generation: generation, kind: .correctionCandidate))
            }
        case let .applied(undoText):
            slots.append(.init(text: undoText, generation: generation, kind: .correctionUndo))
        case nil:
            break
        }
        // A completion that spells the repair, or the word as typed, says nothing
        // the correction slots do not already say.
        for candidate in personal where slots.count < slotCount {
            guard !slots.contains(where: { $0.text == candidate.text }) else { continue }
            slots.append(candidate)
        }
        return slots
    }
}
