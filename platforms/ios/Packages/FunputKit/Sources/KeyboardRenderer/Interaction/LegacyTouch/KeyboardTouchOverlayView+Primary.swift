#if canImport(UIKit)
import KeyboardTouchCore
import UIKit

extension KeyboardTouchOverlayView {
    func capturePrimary(
        _ touches: Set<UITouch>,
        phase: ContactPhase
    ) -> Bool {
        guard pipelineMode == .v2 else { return false }
        let samples = capture(touches, phase: phase)
        onSamples?(samples)
        routePrimary(samples)
        return true
    }

    func captureShadow(_ touches: Set<UITouch>, phase: ContactPhase) {
#if DEBUG
        onSamples?(capture(touches, phase: phase))
#endif
    }

    private func capture(
        _ touches: Set<UITouch>,
        phase: ContactPhase
    ) -> [ContactSample] {
        let unknownBefore = captureAdapter.unknownCallbackCount
        let samples = captureAdapter.samples(
            for: touches,
            phase: phase,
            in: self
        )
        let unknown = captureAdapter.unknownCallbackCount - unknownBefore
        for _ in 0..<unknown { onUnknownCapture?() }
        return samples
    }

    private func routePrimary(_ samples: [ContactSample]) {
        for sample in samples {
            let token = sample.id.rawValue
            let hit = resolvedHit(at: sample.location)
            switch sample.phase {
            case .began:
                if let hit { onBegin?(token, hit, sample.location) }
            case .moved:
                onMove?(token, hit, sample.location)
            case .ended:
                onMove?(token, hit, sample.location)
                onEnd?(token)
            case .cancelled:
                onCancel?(token)
            }
        }
    }
}
#endif
