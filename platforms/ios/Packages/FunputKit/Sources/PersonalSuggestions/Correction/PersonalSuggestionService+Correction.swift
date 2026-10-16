import KeyboardInput
import KeyboardLayout
import KeyboardRenderer

/// What a tap on a correction slot asks the keyboard to do.
public enum CorrectionBarAction: Equatable, Sendable {
    /// Keep the word as typed.
    case refuse(typed: String)
    /// Commit this repair.
    case accept(text: String)
    /// Put back the word the last repair replaced.
    case undo
}

extension PersonalSuggestionService {
    /// `update(_:canQuery:shift:)` with what typo correction says about the word
    /// under the caret. The bar is redrawn when only the correction changed: the
    /// dedupe inside the plain update compares prefixes and knows nothing about it.
    public func update(
        _ update: KeyboardSuggestionInputUpdate,
        canQuery: Bool,
        shift: ShiftState = .lowercase,
        correction: CorrectionPreview?
    ) {
        let changed = correction != self.correction
        // Set before the plain update, so the empty list it publishes on a new
        // prefix already carries the correction slots.
        self.correction = correction
        self.update(update, canQuery: canQuery, shift: shift)
        if changed { publish(visibleCandidates) }
    }

    /// What tapping `candidate` means, or `nil` when the bar has moved on since it
    /// was drawn and the tap must be ignored rather than guessed at.
    public func correctionAction(
        for candidate: KeyboardSuggestionCandidate
    ) -> CorrectionBarAction? {
        guard candidate.generation == generation else { return nil }
        switch (candidate.kind, correction) {
        case let (.correctionTyped, .offered(typed, _)?) where typed == candidate.text:
            return .refuse(typed: typed)
        case let (.correctionCandidate, .offered(_, candidates)?)
            where candidates.contains(candidate.text):
            return .accept(text: candidate.text)
        case let (.correctionUndo, .applied(undoText)?) where undoText == candidate.text:
            return .undo
        default:
            return nil
        }
    }
}
