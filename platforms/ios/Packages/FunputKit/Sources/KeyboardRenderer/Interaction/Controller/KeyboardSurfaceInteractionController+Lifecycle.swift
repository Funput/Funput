#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceInteractionController {
    func endTouch(token: TouchToken) {
        guard let state = touches.removeValue(forKey: token) else { return }
        finishTrackedTouch(token: token, state: state)
    }

    func cancelTouch(token: TouchToken) {
        guard let state = touches.removeValue(forKey: token) else { return }
        cancelTrackedTouch(token: token, state: state)
    }

    /// Teardown: the surface is going away, so pending presses are discarded
    /// rather than routed through `cancelTouch`, which would honour them.
    func cancelAll() {
        alternateHoldController.cancelAll()
        for (token, state) in touches {
            if let key = state.currentKey { setHighlighted(key, false) }
            endSignpost(state, token: token, phase: 2)
        }
        touches.removeAll(keepingCapacity: true)
        clearKeyRepeat()
        refreshPreview()
    }
}
#endif
