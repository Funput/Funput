import CoreGraphics

/// Where a key sits on the ten-column letter grid, for rows Apple lays out by column
/// rather than by relative width.
///
/// Apple's keyboard lines `z`…`m` up under `s`…`l` and starts the spacebar under `x`,
/// whatever widths Shift and the switch key end up with. Weights cannot express that: they
/// share out whatever the row has left, so every key after a wider one drifts. A span pins
/// both edges to the grid instead.
public struct KeyColumnSpan: Hashable, Sendable {
    public enum Anchor: Hashable, Sendable {
        /// The leading edge of the key area, after horizontal padding.
        case leading
        /// The trailing edge of the key area, before horizontal padding.
        case trailing
        /// `columns` grid pitches from the leading edge, plus `gaps` key gaps. A pitch is one
        /// letter key and one gap, so column `n` is where the `n`th letter of the top row starts.
        case grid(columns: CGFloat, gaps: CGFloat = 0)
    }

    public let start: Anchor
    public let end: Anchor

    public init(start: Anchor, end: Anchor) {
        self.start = start
        self.end = end
    }

    /// A single letter-width key starting at `column`.
    public static func letter(at column: CGFloat) -> Self {
        Self(start: .grid(columns: column), end: .grid(columns: column + 1, gaps: -1))
    }
}
