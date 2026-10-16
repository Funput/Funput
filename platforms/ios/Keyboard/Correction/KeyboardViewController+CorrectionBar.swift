import KeyboardInput
import KeyboardRenderer
import PersonalSuggestions
import UIKit

extension KeyboardViewController {
    /// Route a tap on the bar by what the slot stands for: a completion goes to the
    /// personal lexicon as before; a correction slot keeps, commits or takes back
    /// the word under the caret.
    func handleSuggestionTap(_ candidate: KeyboardSuggestionCandidate) {
        guard candidate.kind != .suggestion else {
            acceptPersonalSuggestion(candidate)
            return
        }
        // The bar has moved on since this slot was drawn: ignore the tap rather
        // than act on a word that is no longer there.
        guard let action = personalSuggestionService.correctionAction(for: candidate) else {
            clearPersonalSuggestions()
            return
        }
        let writer = makeDocumentWriter()
        let effects: KeyboardPostCommitEffects
        switch action {
        case let .refuse(typed):
            effects = inputCoordinator.refuseCorrection(typed: typed, writer: writer)
        case let .accept(text):
            effects = inputCoordinator.acceptCorrection(text, writer: writer)
        case .undo:
            effects = inputCoordinator.undoCorrectionFromBar(writer: writer)
        }
        applyPostCommitEffects(effects)
    }
}
