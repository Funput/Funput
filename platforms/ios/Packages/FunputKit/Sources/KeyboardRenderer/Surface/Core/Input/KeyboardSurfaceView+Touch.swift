#if canImport(UIKit)
import UIKit

extension KeyboardSurfaceView {
    func emitTouchEvent(_ event: KeyboardKeyEvent) {
        onKeyEvent?(event)
    }

    func handleContactInteractionEvent(
        token: UInt64,
        event: KeyboardKeyEvent
    ) {
        if let output = touchCoordinator.handleInteraction(token: token, event: event) {
            onKeyEvent?(output)
        }
    }

    func configureTouchPipeline() {
        touchOverlay.onSamples = { [weak self] samples in
            guard let self else { return }
            samples.forEach(touchCoordinator.consume)
            interactionController.commitInitialBackspaces()
        }
        touchOverlay.onUnknownCapture = { [weak self] in
            self?.touchCoordinator.recordUnknownCaptureCallback()
        }
        touchOverlay.onStaleIdentity = { [weak self] in
            self?.touchCoordinator.recordStaleIdentity()
        }
    }

    func claimContactGesture(
        _ token: UInt64,
        kind: KeyboardSurfaceInteractionController.GestureClaim
    ) -> Bool {
        touchCoordinator.claim(token: token, kind: kind)
    }

    func resetTouchPipeline() {
        touchCoordinator.reset()
    }

    /// Forgets every touch in flight: tracked identities, held keys and key repeat,
    /// the preview and the alternates palette.
    func discardTouches() {
        touchOverlay.forgetTrackedTouches()
        interactionController.cancelAll()
        resetTouchPipeline()
    }

    /// Returns the surface to how a new one starts, before it is handed to the
    /// keyboard of another host app. Moving between input views is not guaranteed to
    /// pass through a window-less moment, so the touches are discarded here rather
    /// than left to `didMoveToWindow`: a Backspace held in one app must not go on
    /// deleting in the next. Its new holder pushes its own presentation and callbacks.
    public func resetForReuse() {
        discardTouches()
        updateSuggestions([])
        updateClipboardHint(nil)
        onSuggestionEdit = nil
        isHidden = false
    }

    public func updateSuggestions(_ candidates: [KeyboardSuggestionCandidate]) {
        toolbarView.updateSuggestions(candidates)
    }

    public func updateClipboardHint(_ hint: KeyboardClipboardHint?) {
        toolbarView.updateClipboardHint(hint)
    }

    /// Turned off when the user declines the clipboard feature entirely.
    public func updateClipboardKeyVisible(_ visible: Bool) {
        toolbarView.updateClipboardKeyVisible(visible)
    }

    /// Readies haptics and the key sound. Called once the keyboard is on screen, so
    /// neither the Taptic Engine nor the audio server is woken on the way to the
    /// first frame.
    public func prepareFeedback() {
        interactionController.prepareFeedback(presentation)
    }

    /// Lets go of the loaded key sound while the keyboard is off screen.
    public func releaseFeedback() {
        interactionController.releaseFeedback()
    }

    @objc func accessibilityAppearanceDidChange() {
        applyPresentation()
        setNeedsLayout()
    }
}
#endif
