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
        }
        touchOverlay.onUnknownCapture = { [weak self] in
            self?.touchCoordinator.recordUnknownCaptureCallback()
        }
    }

    func claimContactGesture(
        _ token: UInt64,
        kind: KeyboardSurfaceInteractionController.GestureClaim
    ) -> Bool {
        touchCoordinator.claim(token: token, kind: kind)
    }

    func resetTouchPipeline(flushingResolvedPresses: Bool = false) {
        touchCoordinator.reset(
            flushingResolvedPresses: flushingResolvedPresses
        )
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

    @objc func accessibilityAppearanceDidChange() {
        applyPresentation()
        setNeedsLayout()
    }
}
#endif
