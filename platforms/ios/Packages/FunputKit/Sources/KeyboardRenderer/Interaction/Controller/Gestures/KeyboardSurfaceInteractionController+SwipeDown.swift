#if canImport(UIKit)
import KeyboardLayout
import UIKit

/// Pulling down on an iPad key types its hint, as on the stock keyboard. See
/// `KeySwipeDownTracker` for which keys qualify.
extension KeyboardSurfaceInteractionController {
    func makeSwipeDownTracker(for key: KeySpec, sourceFrame: CGRect?) -> KeySwipeDownTracker? {
        guard let sourceFrame else { return nil }
        return KeySwipeDownTracker(key: key, keyHeight: sourceFrame.height)
    }

    /// Follows the pull; returns whether it holds the finger, which then stays on its key
    /// rather than sliding to the one below.
    func updateSwipeDown(token: TouchToken, translation: CGPoint) -> Bool {
        guard var state = touches[token], var tracker = state.swipeDown else { return false }
        let before = tracker.progress
        guard let progress = tracker.update(translation: translation) else {
            state.swipeDown = tracker
            touches[token] = state
            if before > 0 {
                onSwipeProgress(state.initialKey, 0, true)
                refreshPreview()
            }
            return false
        }
        if progress >= 1, !tracker.claimed {
            // The pipeline may already have committed this press (another finger rolled
            // over it); then the letter stands and the pull is dropped.
            guard onClaimGesture(token, .alternate) else {
                state.swipeDown = nil
                touches[token] = state
                onSwipeProgress(state.initialKey, 0, true)
                refreshPreview()
                return false
            }
            tracker.claimed = true
            performHapticFeedback()
        }
        alternateHoldController.cancel(for: token)
        state.swipeDown = tracker
        touches[token] = state
        if progress != before { onSwipeProgress(state.initialKey, progress, false) }
        if before == 0 { refreshPreview() }
        return true
    }

    /// Ends a claimed pull: the hint when the finger stayed down, the letter when it backed
    /// out. Returns false when no pull owns the contact.
    func finishSwipeDown(token: TouchToken, state: TouchState) -> Bool {
        guard let tracker = state.swipeDown else { return false }
        if tracker.progress > 0 { onSwipeProgress(state.initialKey, 0, true) }
        guard tracker.claimed else { return false }
        let key = state.initialKey
        let alternate = tracker.typesHint ? hintAlternate(for: key) : letterAlternate(for: key)
        if let current = state.currentKey { setHighlighted(current, false) }
        if let alternate {
            onContactEvent(token, KeyboardKeyEvent(key: key, phase: .alternateSelected(alternate)))
        } else {
            onContactEvent(token, KeyboardKeyEvent(key: key, phase: .cancelled))
        }
        endSignpost(state, token: token, phase: 3)
        refreshPreview()
        return true
    }

    /// The hint as the key's own alternate when it has one, so it types exactly what the
    /// long-press palette would; a punctuation hint never turns uppercase under Shift.
    func hintAlternate(for key: KeySpec) -> KeyAlternate? {
        guard let hint = key.secondaryLabel, !hint.isEmpty else { return nil }
        return key.alternates.first { $0.text == hint }
            ?? KeyAlternate(text: hint, shiftedText: hint)
    }

    private func letterAlternate(for key: KeySpec) -> KeyAlternate? {
        guard !key.label.isEmpty else { return nil }
        return KeyAlternate(text: key.label, shiftedText: key.shiftedLabel)
    }
}
#endif
