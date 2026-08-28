#if canImport(UIKit)
/// One tick of the spacebar trackpad, expressed in document units rather than points.
///
/// Both axes travel in the same step: the caret follows the finger the way the system
/// keyboard's trackpad does, instead of locking to whichever axis happened to move first.
public struct CursorPanStep: Equatable, Sendable {
    /// Characters to move within the line. Positive moves right.
    public var columns: Int
    /// Lines to move. Logical lines — separated by a newline, not by soft wrapping, which
    /// a keyboard extension cannot see. Positive moves down.
    public var lines: Int

    public init(columns: Int = 0, lines: Int = 0) {
        self.columns = columns
        self.lines = lines
    }

    public var isEmpty: Bool { columns == 0 && lines == 0 }
}
#endif
