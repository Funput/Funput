import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    /// The keycaps run to both screen edges and sit just above the home indicator, which is
    /// where the system's own edge gestures wait before handing a touch over. Asking for the
    /// keyboard's touches to win there keeps `q`, `a`, `p`, `l`, Shift and Delete as prompt as
    /// the keys in the middle. The Home gesture itself cannot be deferred on Face ID iPhones.
    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge {
        [.left, .right, .bottom]
    }

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
        case let .cursorMoved(offset):
            applyPostCommitEffects(
                inputCoordinator.moveCursor(by: offset, writer: makeDocumentWriter())
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
