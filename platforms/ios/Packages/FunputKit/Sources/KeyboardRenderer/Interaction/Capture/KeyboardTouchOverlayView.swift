#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchCore
import KeyboardTouchUIKit
import UIKit

/// One multi-touch surface for all keycaps. Central tracking lets a finger move
/// across keys without depending on button-style `touchUpInside` delivery.
@MainActor
final class KeyboardTouchOverlayView: UIView {
    typealias TouchToken = UInt64
    typealias Hit = (key: KeySpec, frame: CGRect)

    var onBegin: ((TouchToken, Hit, CGPoint) -> Void)?
    var onMove: ((TouchToken, Hit?, CGPoint) -> Void)?
    var onEnd: ((TouchToken) -> Void)?
    var onCancel: ((TouchToken) -> Void)?
    var onSamples: (([ContactSample]) -> Void)?
    var onUnknownCapture: (() -> Void)?

    /// Borrowed from the pipeline, not rebuilt: the exact snapshot new contacts commit
    /// against, so a press can never highlight one key and commit another.
    private var geometry: KeyboardGeometrySnapshot?
    let captureAdapter = UIKitTouchCaptureAdapter()

    override init(frame: CGRect) {
        super.init(frame: frame)
        isMultipleTouchEnabled = true
        isOpaque = false
        backgroundColor = .clear
        isAccessibilityElement = false
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func adoptGeometry(_ snapshot: KeyboardGeometrySnapshot?) {
        geometry = snapshot
    }

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        geometry?.trackingRegion.contains(point) ?? false
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        captureAndRoute(touches, phase: .began)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        captureAndRoute(touches, phase: .moved)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        captureAndRoute(touches, phase: .ended)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        captureAndRoute(touches, phase: .cancelled)
    }

    /// Drops the overlay's bookkeeping without reporting anything upward. Both
    /// call sites pair this with the controller's own `cancelAll()`, which is what
    /// discards the pending presses; reporting them as cancellations instead would
    /// now commit them.
    func forgetTrackedTouches() {
        captureAdapter.reset()
    }

    func resolvedHit(at point: CGPoint) -> Hit? {
        geometry?.touchHit(at: point).map { ($0.key, $0.frame) }
    }
}
#endif
