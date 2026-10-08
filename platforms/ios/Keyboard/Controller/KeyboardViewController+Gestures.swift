import KeyboardInput
import KeyboardLayout
import KeyboardRenderer
import os
import UIKit

extension KeyboardViewController {
    /// The keycaps run to both screen edges and sit just above the home indicator, which is
    /// where the system's own edge gestures wait before handing a touch over. Asking for the
    /// keyboard's touches to win there keeps `q`, `a`, `p`, `l`, Shift and Delete as prompt as
    /// the keys in the middle. The Home gesture itself cannot be deferred on Face ID iPhones.
    override var preferredScreenEdgesDeferringSystemGestures: UIRectEdge {
        [.left, .right, .bottom]
    }

    /// Stops the system's edge-gesture gates from holding back `touchesBegan`.
    ///
    /// The override above is not enough inside a keyboard extension: UIKit asks the root
    /// view controller of the extension's window, which belongs to the system, so the
    /// gates on that window keep `delaysTouchesBegan` on. A finger resting on Delete at the
    /// right edge then reaches the keyboard only once the gate gives up, which delayed both
    /// the first deletion and key repeat (#516). The gates still recognize edge swipes and
    /// cancel the touch when they do; they just stop withholding its first sample.
    func releaseSystemGestureTouchDelay() {
        guard let recognizers = view.window?.gestureRecognizers else { return }
        for recognizer in recognizers where recognizer.delaysTouchesBegan {
#if DEBUG
            os_log(
                .info,
                log: KeyboardControllerSignpost.log,
                "Released touch delay on window recognizer %{public}@",
                String(describing: type(of: recognizer))
            )
#endif
            recognizer.delaysTouchesBegan = false
        }
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
