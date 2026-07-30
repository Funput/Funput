#if canImport(UIKit)
import UIKit

extension KeyboardSurfaceView {
    func handleV2Event(_ event: KeyboardKeyEvent) {
#if DEBUG
        recordActualDiagnostic(event)
#endif
        onKeyEvent?(event)
    }

    func handleContactInteractionEvent(
        token: UInt64,
        event: KeyboardKeyEvent
    ) {
#if DEBUG
        recordActualDiagnostic(event)
#endif
        if let output = v2Touch.handleInteraction(token: token, event: event) {
            onKeyEvent?(output)
        }
    }

    func configureTouchPipeline() {
        touchOverlay.onSamples = { [weak self] samples in
            guard let self else { return }
#if DEBUG
            samples.forEach(touchShadow.consume)
#endif
            samples.forEach(v2Touch.consume)
        }
        touchOverlay.onUnknownCapture = { [weak self] in
#if DEBUG
            self?.touchShadow.recordUnknownCaptureCallback()
#endif
        }
    }

    func claimContactGesture(
        _ token: UInt64,
        kind: KeyboardSurfaceInteractionController.GestureClaim
    ) {
        v2Touch.claim(token: token, kind: kind)
#if DEBUG
        touchShadow.excludeFromComparison(token)
#endif
    }

    func resetTouchPipeline() {
        v2Touch.reset()
#if DEBUG
        touchShadow.reset()
#endif
    }

#if DEBUG
    var touchShadowResolvedCount: Int {
        touchShadow.trace.metrics.shadowResolved
    }

    var touchShadowMatchCount: Int {
        touchShadow.trace.metrics.matched
    }
#endif

#if DEBUG
    private func recordActualDiagnostic(_ event: KeyboardKeyEvent) {
        switch event.phase {
        case .released: touchShadow.recordActualRelease(event.key)
        case .cancelled: touchShadow.recordActualCancellation(event.key)
        default: break
        }
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
