import CoreGraphics
import KeyboardLayout

public struct KeyboardGeometrySnapshot: Sendable {
    private struct Entry: Sendable {
        let key: ResolvedKey
    }

    public let revision: UInt64
    public let geometry: ResolvedKeyboard
    private let entries: [Entry]
    private let trackingBounds: CGRect

    public init(revision: UInt64, geometry: ResolvedKeyboard) {
        self.revision = revision
        self.geometry = geometry
        entries = geometry.keys.map { key in
            Entry(key: key)
        }
        let union = geometry.keys.reduce(CGRect.null) { $0.union($1.frame) }
        trackingBounds = union.insetBy(dx: -12, dy: -12)
    }

    public func touchHit(at point: CGPoint) -> KeyboardTouchHit? {
        guard trackingBounds.contains(point) else { return nil }
        var closest: Entry?
        var closestDistance = CGFloat.greatestFiniteMagnitude
        for entry in entries where entry.key.spec.role != .placeholder {
            let frame = entry.key.frame
            let dx = max(max(frame.minX - point.x, 0), point.x - frame.maxX)
            let dy = max(max(frame.minY - point.y, 0), point.y - frame.maxY)
            let distance = dx * dx + dy * dy
            if distance < closestDistance {
                closest = entry
                closestDistance = distance
            }
        }
        return closest.map {
            KeyboardTouchHit(key: $0.key.spec, frame: $0.key.frame)
        }
    }
}
