#if canImport(UIKit) && DEBUG
import KeyboardTouchCore
import KeyboardTouchUIKit
import UIKit

@MainActor
final class KeyboardTouchShadowCaptureBridge {
    var onSamples: (([ContactSample]) -> Void)?
    var onUnknownCallback: (() -> Void)?

    private let adapter = UIKitTouchCaptureAdapter()

    func capture(
        _ touches: Set<UITouch>,
        phase: ContactPhase,
        in view: UIView
    ) {
        let unknownBefore = adapter.unknownCallbackCount
        let samples = adapter.samples(for: touches, phase: phase, in: view)
        if !samples.isEmpty { onSamples?(samples) }
        let unknownCount = adapter.unknownCallbackCount - unknownBefore
        for _ in 0..<unknownCount { onUnknownCallback?() }
    }

    func reset() {
        adapter.reset()
    }
}
#endif
