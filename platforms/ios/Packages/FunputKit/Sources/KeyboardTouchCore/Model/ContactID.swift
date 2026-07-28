public struct ContactID: RawRepresentable, Hashable, Comparable, Sendable {
    public let rawValue: UInt64

    public init(rawValue: UInt64) {
        self.rawValue = rawValue
    }

    public static func < (lhs: ContactID, rhs: ContactID) -> Bool {
        lhs.rawValue < rhs.rawValue
    }
}
