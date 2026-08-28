#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardSurfaceInteractionController {
    /// Hold timer callback: the press is now old enough to become a trackpad, but nothing
    /// is claimed until the finger actually moves — a resting thumb must still type a space.
    func armSpaceTrackpad(for token: TouchToken) {
        touches[token]?.holdArmed = true
    }

    /// Promotes an armed space press into a caret pan once the finger travels.
    ///
    /// Distance is measured in any direction, not just sideways: an upward drag is how the
    /// caret reaches the line above, and it has no other meaning on the spacebar.
    ///
    /// A refused claim means the pipeline already committed the press; the caller falls
    /// through to the ordinary swipe handling rather than emitting anyway.
    func activateSpaceTrackpad(token: TouchToken, translation: CGPoint) -> Bool {
        guard var state = touches[token],
              state.holdArmed,
              state.trackpad == nil,
              hypot(translation.x, translation.y)
                  >= KeyboardSurfaceInteractionController.trackpadActivation,
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
        let step = tracker.update(translation: translation)
        state.trackpad = tracker
        touches[token] = state
        guard !step.isEmpty else { return }
        onContactEvent(
            token,
            KeyboardKeyEvent(key: state.initialKey, phase: .cursorMoved(step))
        )
        // A line change moves the caret further than the user can track from the finger
        // alone, so it gets the firmer tick and stays distinguishable without looking.
        if hapticsEnabled { haptics.perform(step.lines == 0 ? .deleteRepeat : .control) }
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
