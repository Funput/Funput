#if canImport(UIKit)
import KeyboardTouchCore
import UIKit

extension KeyboardTouchOverlayView {
    /// Runs one UIKit callback in three ordered phases.
    ///
    /// Recognition has to come before the commit: a swipe that crosses its threshold on the
    /// final move must claim the contact while the pipeline still owns it, otherwise the key
    /// underneath commits first and the gesture is lost. Termination comes last so the
    /// controller tears a contact down only after its press has been accounted for.
    func captureAndRoute(
        _ touches: Set<UITouch>,
        phase: ContactPhase
    ) {
        let samples = capture(touches, phase: phase)
        recognize(samples)
        onSamples?(samples)
        finish(samples)
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

    /// Presentation and gesture tracking. Never terminates a contact.
    private func recognize(_ samples: [ContactSample]) {
        for sample in samples {
            let token = sample.id.rawValue
            switch sample.phase {
            case .began:
                if let hit = resolvedHit(at: sample.location) {
                    onBegin?(token, hit, sample.location)
                }
            case .moved, .ended:
                onMove?(token, resolvedHit(at: sample.location), sample.location)
            case .cancelled:
                break
            }
        }
    }

    /// Terminal bookkeeping, after the pipeline has consumed the same samples.
    private func finish(_ samples: [ContactSample]) {
        for sample in samples {
            let token = sample.id.rawValue
            switch sample.phase {
            case .ended:
                onEnd?(token)
            case .cancelled:
                onCancel?(token)
            case .began, .moved:
                break
            }
        }
    }
}
#endif
