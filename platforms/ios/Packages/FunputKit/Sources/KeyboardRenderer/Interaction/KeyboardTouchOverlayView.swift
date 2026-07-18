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
    private var tokens: [ObjectIdentifier: TouchToken] = [:]
    private var nextToken: TouchToken = 1

    override init(frame: CGRect) {
        super.init(frame: frame)
        isMultipleTouchEnabled = true
        isOpaque = false
        backgroundColor = .clear
        isAccessibilityElement = false
        tokens.reserveCapacity(10)
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
            let identifier = ObjectIdentifier(touch)
            guard tokens[identifier] == nil else { continue }
            let token = nextToken
            nextToken &+= 1
            tokens[identifier] = token
            onBegin?(token, hit, point)
        }
        reconcile(event)
    }

    override func touchesMoved(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            guard let token = tokens[ObjectIdentifier(touch)] else { continue }
            let point = touch.location(in: self)
            onMove?(token, trackingBounds.contains(point) ? hit(at: point) : nil, point)
        }
        reconcile(event)
    }

    override func touchesEnded(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            let identifier = ObjectIdentifier(touch)
            guard let token = tokens.removeValue(forKey: identifier) else { continue }
            let point = touch.location(in: self)
            onMove?(token, trackingBounds.contains(point) ? hit(at: point) : nil, point)
            onEnd?(token)
        }
        reconcile(event)
    }

    override func touchesCancelled(_ touches: Set<UITouch>, with event: UIEvent?) {
        for touch in touches {
            guard let token = tokens.removeValue(forKey: ObjectIdentifier(touch)) else { continue }
            onCancel?(token)
        }
        reconcile(event)
    }

    func cancelAllTrackedTouches() {
        let active = Array(tokens.values)
        tokens.removeAll(keepingCapacity: true)
        active.forEach { onCancel?($0) }
        onReconcile?([])
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
        guard let allTouches = event?.allTouches else {
            onReconcile?(Set(tokens.values))
            return
        }
        let activeIdentifiers = Set(allTouches.lazy.filter {
            $0.phase == .began || $0.phase == .moved || $0.phase == .stationary
        }.map { ObjectIdentifier($0) })
        let activeTokens = Set(tokens.compactMap { identifier, token in
            activeIdentifiers.contains(identifier) ? token : nil
        })
        onReconcile?(activeTokens)
    }
}
#endif
