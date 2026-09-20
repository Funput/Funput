/// Where a finger landed, relative to the keys around it.
///
/// Distances are in **key pitches** — one key plus one gap — so the same numbers mean
/// the same thing on every screen size, which is what lets the Rust engine score them
/// without knowing anything about the layout.
///
/// Three neighbours at most, and only ones the finger could plausibly have meant:
/// offering a key the touch was nowhere near costs the engine a whole tier of its
/// replay search and buys nothing. A key gathered here is named by the scalar its cap
/// shows, in lower case — the engine matches the case of the key it actually
/// received, so the caller does not have to reason about Shift.
public struct KeyboardTouchEvidence: Sendable, Equatable {
    /// A key the same touch could have meant.
    public struct Alternate: Sendable, Equatable {
        public let scalar: Unicode.Scalar
        public let distance: Float

        public init(scalar: Unicode.Scalar, distance: Float) {
            self.scalar = scalar
            self.distance = distance
        }
    }

    /// Past this a neighbour is not a plausible reading of the same touch.
    public static let plausibleDistance: Float = 1.2

    public let typed: Unicode.Scalar
    public let typedDistance: Float
    public let first: Alternate?
    public let second: Alternate?
    public let third: Alternate?

    public init(
        typed: Unicode.Scalar,
        typedDistance: Float,
        first: Alternate? = nil,
        second: Alternate? = nil,
        third: Alternate? = nil
    ) {
        self.typed = typed
        self.typedDistance = typedDistance
        self.first = first
        self.second = second
        self.third = third
    }

    /// The alternates in order, without building an array — this runs once per
    /// keystroke on the input path.
    public func forEachAlternate(_ body: (Alternate) -> Void) {
        if let first { body(first) }
        if let second { body(second) }
        if let third { body(third) }
    }
}
