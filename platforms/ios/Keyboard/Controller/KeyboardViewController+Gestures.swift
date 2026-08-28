import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    /// Handles the phases the gesture lane writes to the document itself.
    ///
    /// Returns whether the event was fully handled, so `handleKeyEvent` can leave the
    /// ordinary key path untouched.
    func handleGesturePhase(_ phase: KeyboardKeyEvent.Phase) -> Bool {
        switch phase {
        case .swiped(.toggleLanguage):
            inputCoordinator.toggleLanguage()
            applyPostCommitEffects(
                .init(presentationChanged: true, suggestionsChanged: true)
            )
        case let .cursorMoved(step):
            applyPostCommitEffects(
                inputCoordinator.moveCaret(
                    columns: step.columns,
                    lines: step.lines,
                    writer: makeDocumentWriter()
                )
            )
        case .deletedWord:
            applyPostCommitEffects(
                inputCoordinator.deleteWordBackward(writer: makeDocumentWriter())
            )
        default:
            return false
        }
        return true
    }
}
