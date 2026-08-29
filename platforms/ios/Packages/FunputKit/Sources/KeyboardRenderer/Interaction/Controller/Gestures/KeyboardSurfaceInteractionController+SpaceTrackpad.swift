#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceInteractionController {
    /// Hold timer callback: the press is now old enough to become a trackpad, but nothing
    /// is claimed until the finger actually moves — a resting thumb must still type a space.
    func armSpaceTrackpad(for token: TouchToken) {
        touches[token]?.holdArmed = true
    }

    /// Promotes an armed space press into a caret pan once the finger travels sideways.
    ///
    /// A refused claim means the pipeline already committed the press; the caller falls
    /// through to the ordinary swipe handling rather than emitting anyway.
    func activateSpaceTrackpad(token: TouchToken, translation: CGPoint) -> Bool {
        guard var state = touches[token],
              state.holdArmed,
              state.trackpad == nil,
              abs(translation.x) >= KeyboardSurfaceInteractionController.trackpadActivation,
              // A repeat that already fired inserted spaces; switching to a caret pan on
              // top of that would leave the user unsure what the gesture did.
              !(repeatTouch == token && repeatController.hasRepeated),
              onClaimGesture(token, .trackpad)
        else { return false }
        clearKeyRepeat()
        if let key = state.currentKey { setHighlighted(key, false) }
        state.currentKey = nil
        state.claimedGesture = .trackpad
        state.trackpad = SpaceCursorPanTracker()
        touches[token] = state
        if hapticsEnabled { haptics.perform(.control) }
        refreshPreview()
        return true
    }

    func updateSpaceTrackpad(token: TouchToken, translation: CGPoint) {
        guard var state = touches[token], var tracker = state.trackpad else { return }
        let offset = tracker.update(translationX: translation.x)
        state.trackpad = tracker
        touches[token] = state
        guard offset != 0 else { return }
        onContactEvent(
            token,
            KeyboardKeyEvent(key: state.initialKey, phase: .cursorMoved(offset: offset))
        )
        if hapticsEnabled { haptics.perform(.deleteRepeat) }
    }

    func completeGestureTouch(token: TouchToken, state: TouchState) {
        touches.removeValue(forKey: token)
        if let key = state.currentKey { setHighlighted(key, false) }
        if repeatTouch == token { clearKeyRepeat() }
        // One `.cancelled` closes the contact's bookkeeping without producing a key: the
        // gesture already wrote whatever the user asked for.
        onContactEvent(
            token,
            KeyboardKeyEvent(key: state.initialKey, phase: .cancelled)
        )
        endSignpost(state, token: token, phase: 3)
        refreshPreview()
    }
}
#endif
