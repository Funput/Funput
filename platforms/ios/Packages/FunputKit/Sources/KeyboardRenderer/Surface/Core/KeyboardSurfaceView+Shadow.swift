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

    func handleContactInteractionEvent(
        token: UInt64,
        event: KeyboardKeyEvent
    ) {
#if DEBUG
        recordLegacyDiagnostic(event)
#endif
        if touchPipelineMode == .primaryFastTap {
            if let output = primaryTouch.handleLegacy(token: token, event: event) {
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
            if touchPipelineMode == .primaryFastTap {
                samples.forEach(primaryTouch.consume)
            }
        }
        touchOverlay.onUnknownCapture = { [weak self] in
#if DEBUG
            self?.touchShadow.recordUnknownCaptureCallback()
#endif
        }
    }

    func promoteContactToLegacy(_ token: UInt64) {
        primaryTouch.promote(token: token)
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
#if !DEBUG
        guard mode == .legacy else { return false }
#endif
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
