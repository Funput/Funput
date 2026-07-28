import CoreGraphics
import KeyboardLayout

public struct KeyboardGeometrySnapshot: Sendable {
    private struct Entry: Sendable {
        let key: ResolvedKey
        let identity: ShadowKeyIdentity
    }

    public let revision: UInt64
    public let geometry: ResolvedKeyboard
    private let entries: [Entry]
    private let trackingBounds: CGRect
    private let identitiesByKeyID: [String: ShadowKeyIdentity]

    public init(revision: UInt64, geometry: ResolvedKeyboard) {
        self.revision = revision
        self.geometry = geometry
        var identities: [String: ShadowKeyIdentity] = [:]
        entries = geometry.keys.enumerated().map { ordinal, key in
            let identity = ShadowKeyIdentity(
                geometryRevision: revision,
                ordinal: ordinal,
                role: key.spec.role
            )
            identities[key.spec.id] = identity
            return Entry(key: key, identity: identity)
        }
        identitiesByKeyID = identities
        let union = geometry.keys.reduce(CGRect.null) { $0.union($1.frame) }
        trackingBounds = union.insetBy(dx: -12, dy: -12)
    }

    public func hit(at point: CGPoint) -> ShadowKeyIdentity? {
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
        return closest?.identity
    }

    public func identity(for key: KeySpec) -> ShadowKeyIdentity? {
        identitiesByKeyID[key.id]
    }
}
