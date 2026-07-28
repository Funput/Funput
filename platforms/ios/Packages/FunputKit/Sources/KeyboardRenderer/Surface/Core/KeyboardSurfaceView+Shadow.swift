#if canImport(UIKit)
import UIKit

extension KeyboardSurfaceView {
    func handleInteractionEvent(_ event: KeyboardKeyEvent) {
#if DEBUG
        switch event.phase {
        case .released:
            touchShadow.recordLegacyRelease(event.key)
        case .cancelled:
            touchShadow.recordLegacyCancellation(event.key)
        case .pressed, .repeated, .swiped, .alternateSelected:
            break
        }
#endif
        onKeyEvent?(event)
    }

#if DEBUG
    func configureTouchShadow() {
        touchOverlay.shadowCapture.onSamples = { [weak self] samples in
            guard let self else { return }
            samples.forEach(touchShadow.consume)
        }
        touchOverlay.shadowCapture.onUnknownCallback = { [weak self] in
            self?.touchShadow.recordUnknownCaptureCallback()
        }
    }

    func resetTouchShadow() {
        touchOverlay.resetShadowCapture()
        touchShadow.reset()
    }

    var touchShadowResolvedCount: Int {
        touchShadow.trace.metrics.shadowResolved
    }

    var touchShadowMatchCount: Int {
        touchShadow.trace.metrics.matched
    }
#endif

    public func updateSuggestions(_ candidates: [KeyboardSuggestionCandidate]) {
        toolbarView.updateSuggestions(candidates)
    }

    @objc func accessibilityAppearanceDidChange() {
        applyPresentation()
        setNeedsLayout()
    }
}
#endif
