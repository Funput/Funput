#if canImport(UIKit)
import KeyboardLayout
import UIKit

/// One multi-touch surface for all keycaps. Central tracking lets a finger move
/// across keys without depending on button-style `touchUpInside` delivery.
@MainActor
final class KeyboardTouchOverlayView: UIView {
    typealias TouchToken = KeyboardPressCommitQueue.TouchToken
    typealias Hit = (key: KeySpec, frame: CGRect)

    var onBegin: ((TouchToken, Hit, CGPoint) -> Void)?
    var onMove: ((TouchToken, Hit?, CGPoint) -> Void)?
    var onEnd: ((TouchToken) -> Void)?
    var onCancel: ((TouchToken) -> Void)?
    var onReconcile: ((Set<TouchToken>) -> Void)?

    private static let outerTolerance: CGFloat = 12
    private var keys: [ResolvedKey] = []
    private var trackingBounds = CGRect.null
    private var ledger = KeyboardTouchTokenLedger()

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

    func updateGeometry(_ geometry: ResolvedKeyboard) {
        keys = geometry.keys
        let union = keys.reduce(CGRect.null) { $0.union($1.frame) }
        trackingBounds = union.insetBy(
            dx: -Self.outerTolerance,
            dy: -Self.outerTolerance
        )
    }

    override func point(inside point: CGPoint, with event: UIEvent?) -> Bool {
        trackingBounds.contains(point)
    }

    override func touchesBegan(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            let point = touch.location(in: self)
            guard let hit = hit(at: point) else { continue }
            let (token, stale) = ledger.beginToken(for: touch)
            // Retire a stale mapping rather than skipping the press as a duplicate:
            // UIKit recycles touch objects, so a leftover entry must never cost the
            // user a keystroke.
            if let stale { onCancel?(stale) }
            onBegin?(token, hit, point)
        }
        reconcile(event)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            guard let token = ledger.token(for: touch) else { continue }
            let point = touch.location(in: self)
            onMove?(token, trackingBounds.contains(point) ? hit(at: point) : nil, point)
        }
        reconcile(event)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            guard let token = ledger.removeToken(for: touch) else { continue }
            let point = touch.location(in: self)
            onMove?(token, trackingBounds.contains(point) ? hit(at: point) : nil, point)
            onEnd?(token)
        }
        reconcile(event)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            guard let token = ledger.removeToken(for: touch) else { continue }
            onCancel?(token)
        }
        reconcile(event)
    }

    /// Drops the overlay's bookkeeping without reporting anything upward. Both
    /// call sites pair this with the controller's own `cancelAll()`, which is what
    /// discards the pending presses; reporting them as cancellations instead would
    /// now commit them.
    func forgetTrackedTouches() {
        ledger.removeAllTokens()
    }

    func resolvedHit(at point: CGPoint) -> Hit? {
        hit(at: point)
    }

    private func hit(at point: CGPoint) -> Hit? {
        guard trackingBounds.contains(point), !keys.isEmpty else { return nil }
        var best: ResolvedKey?
        var bestDistance = CGFloat.greatestFiniteMagnitude
        for key in keys where key.spec.role != .placeholder {
            let dx = max(max(key.frame.minX - point.x, 0), point.x - key.frame.maxX)
            let dy = max(max(key.frame.minY - point.y, 0), point.y - key.frame.maxY)
            let distance = dx * dx + dy * dy
            if distance < bestDistance {
                bestDistance = distance
                best = key
            }
        }
        return best.map { ($0.spec, $0.frame) }
    }

    private func reconcile(_ event: UIEvent?) {
        guard let event, let allTouches = event.allTouches else {
            onReconcile?(ledger.trackedTokens)
            return
        }
        onReconcile?(ledger.survivors(in: allTouches, at: event.timestamp))
    }
}
#endif
