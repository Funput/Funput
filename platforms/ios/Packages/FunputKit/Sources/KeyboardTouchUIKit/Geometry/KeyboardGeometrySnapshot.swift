import CoreGraphics
import KeyboardLayout

public struct KeyboardGeometrySnapshot: Sendable {
    public let revision: UInt64
    public let geometry: ResolvedKeyboard
    private let keys: [ResolvedKey]
    private let rowBands: KeyboardRowBands
    private let trackingBounds: CGRect

    public init(revision: UInt64, geometry: ResolvedKeyboard) {
        self.revision = revision
        self.geometry = geometry
        keys = geometry.keys.filter { $0.spec.role != .placeholder }
        rowBands = KeyboardRowBands(rows: geometry.rows)
        trackingBounds = KeyboardTrackingBounds.resolve(for: geometry)
    }

    /// The region this snapshot answers hits for. The capture view routes touches with it, so
    /// both sides agree on what belongs to the keycaps.
    public var trackingRegion: CGRect { trackingBounds }

    /// Resolves a touch to a keycap, row first.
    ///
    /// A finger inside a row's vertical band means a key on that row, even when a key on the
    /// neighbouring row is closer in a straight line. That happens at the rim of any inset row:
    /// left of `a` the nearest keycap is `q` above or `shift` below, never `a` — and landing on
    /// a modifier produces no character, which is indistinguishable from a dropped key.
    ///
    /// Row gaps are divided at their vertical midpoint. Only the slack above the first row and
    /// below the last falls back to a plain nearest-key search.
    public func touchHit(at point: CGPoint) -> KeyboardTouchHit? {
        guard trackingBounds.contains(point) else { return nil }
        let candidates = rowBands.keys(containing: point.y) ?? keys
        return nearest(to: point, among: candidates).map {
            KeyboardTouchHit(key: $0.spec, frame: $0.frame)
        }
    }

    private func nearest(to point: CGPoint, among candidates: [ResolvedKey]) -> ResolvedKey? {
        var closest: ResolvedKey?
        var closestDistance = CGFloat.greatestFiniteMagnitude
        for key in candidates {
            let frame = key.frame
            let dx = max(max(frame.minX - point.x, 0), point.x - frame.maxX)
            let dy = max(max(frame.minY - point.y, 0), point.y - frame.maxY)
            let distance = dx * dx + dy * dy
            if distance < closestDistance {
                closest = key
                closestDistance = distance
            }
        }
        return closest
    }
}
