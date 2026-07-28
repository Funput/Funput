import KeyboardLayout

public struct ShadowKeyIdentity: Equatable, Hashable, Sendable {
    public let geometryRevision: UInt64
    public let ordinal: Int
    public let role: KeyRole

    public init(geometryRevision: UInt64, ordinal: Int, role: KeyRole) {
        self.geometryRevision = geometryRevision
        self.ordinal = ordinal
        self.role = role
    }
}
