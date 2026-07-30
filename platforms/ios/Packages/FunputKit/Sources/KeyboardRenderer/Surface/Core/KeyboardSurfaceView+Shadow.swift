#if canImport(UIKit)
import UIKit

extension KeyboardSurfaceView {
    func handleV2Event(_ event: KeyboardKeyEvent) {
#if DEBUG
        recordLegacyDiagnostic(event)
#endif
        onKeyEvent?(event)
    }

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

    func handleContactInteractionEvent(
        token: UInt64,
        event: KeyboardKeyEvent
    ) {
#if DEBUG
        recordLegacyDiagnostic(event)
#endif
        if touchPipelineMode == .v2 {
            if let output = primaryTouch.handleInteraction(token: token, event: event) {
                onKeyEvent?(output)
            }
        } else {
            onKeyEvent?(event)
        }
        applyPendingTouchPipelineModeIfIdle()
    }

    func configureTouchPipeline() {
        touchOverlay.onSamples = { [weak self] samples in
            guard let self else { return }
#if DEBUG
            samples.forEach(touchShadow.consume)
#endif
            if touchPipelineMode == .v2 {
                samples.forEach(primaryTouch.consume)
            }
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
        primaryTouch.claim(token: token, kind: kind)
#if DEBUG
        touchShadow.promoteToLegacy(token)
#endif
    }

    func resetTouchPipeline() {
        primaryTouch.reset()
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

    @discardableResult
    public func setTouchPipelineMode(_ mode: KeyboardTouchPipelineMode) -> Bool {
        pendingTouchPipelineMode = mode
        return applyPendingTouchPipelineModeIfIdle()
    }

    @discardableResult
    func applyPendingTouchPipelineModeIfIdle() -> Bool {
        guard interactionController.activeTouchCount == 0,
              primaryTouch.activeContactCount == 0,
              let mode = pendingTouchPipelineMode else { return false }
        pendingTouchPipelineMode = nil
        guard mode != touchPipelineMode else { return true }
        primaryTouch.reset()
        touchOverlay.setPipelineMode(mode)
        interactionController.usesLegacyTouchOutput = mode == .legacy
        touchPipelineMode = mode
        return true
    }

#if DEBUG
    private func recordLegacyDiagnostic(_ event: KeyboardKeyEvent) {
        switch event.phase {
        case .released: touchShadow.recordLegacyRelease(event.key)
        case .cancelled: touchShadow.recordLegacyCancellation(event.key)
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
