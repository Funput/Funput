#if canImport(UIKit)
import Foundation
import KeyboardLayout

extension KeyboardSurfaceInteractionController {
    /// How long Backspace waits before repeating while smart gestures are on.
    ///
    /// Repeating locks out the word rub, and people hold a beat before they rub. The first
    /// character already goes on contact, so the longer wait costs no feedback. The system
    /// edge gates used to hide this race by delaying the contact itself (#516).
    static let smartBackspaceRepeatDelay: TimeInterval = 0.7

    /// Space keeps its hold-to-pan arming. Backspace starts its repeat timer, on the longer
    /// delay when the word rub is enabled, and marks itself for one immediate delete once
    /// the pipeline owns the contact.
    func armHeldKey(token: TouchToken, key: KeySpec, smartGestures: Bool) {
        if smartGestures, key.role == .space {
            // Holding space is how the caret pan starts, so the spacebar does not repeat while
            // smart gestures are on. It used to, on a longer delay, which made the gesture a
            // race the user had to win: hold a beat too long before dragging and the repeat
            // had already typed spaces and locked the pan out. Lifting without dragging still
            // types the one space, because arming is not claiming.
            spaceHoldController.start(for: token)
            return
        }
        guard repeatTouch == nil, key.role == .backspace || key.role == .space else { return }
        repeatTouch = token
        let delaysForRub = smartGestures && key.role == .backspace
        repeatController.start(
            initialDelay: delaysForRub ? Self.smartBackspaceRepeatDelay : nil
        )
        guard key.role == .backspace, var state = touches[token] else { return }
        state.awaitingInitialDelete = true
        touches[token] = state
    }

    /// Deletes one character on Backspace after the pipeline has recorded the contact.
    ///
    /// Calling this from `beginTouch` loses the claim: the pipeline has not consumed the
    /// began sample yet, so detach fails and the later release deletes a second time.
    func commitInitialBackspaces() {
        let pending = touches.compactMap { token, state in
            state.awaitingInitialDelete ? token : nil
        }
        pending.forEach(commitInitialBackspace)
    }

    private func commitInitialBackspace(_ token: TouchToken) {
        guard var state = touches[token], state.awaitingInitialDelete else { return }
        state.awaitingInitialDelete = false
        guard state.initialKey.role == .backspace,
              onClaimGesture(token, .repeatKey) else {
            touches[token] = state
            return
        }
        state.committedInitialDelete = true
        state.claimedGesture = .repeatKey
        touches[token] = state
        onContactEvent(
            token,
            KeyboardKeyEvent(key: state.initialKey, phase: .repeated)
        )
    }
}
#endif
