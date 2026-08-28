#if canImport(UIKit)
import CoreGraphics

/// Converts travel on the spacebar into whole caret steps on both axes.
///
/// Keeps the sub-step remainder per axis so a slow drag advances exactly once per step
/// instead of drifting, and so reversing direction costs nothing.
///
/// Vertical steps are the coarser of the two on purpose: a column is one character, while
/// a line is a whole paragraph jump the user has to read to confirm.
struct SpaceCursorPanTracker {
    private let stepWidth: CGFloat
    private let stepHeight: CGFloat
    private var emittedColumns = 0
    private var emittedLines = 0

    init(stepWidth: CGFloat = 10, stepHeight: CGFloat = 24) {
        precondition(stepWidth > 0)
        precondition(stepHeight > 0)
        self.stepWidth = stepWidth
        self.stepHeight = stepHeight
    }

    /// - Parameter translation: total travel since the pan began, not since the last call.
    /// - Returns: the caret step to apply now; empty when the finger has not crossed into
    ///   a new step on either axis.
    mutating func update(translation: CGPoint) -> CursorPanStep {
        let columns = Self.steps(translation.x, per: stepWidth)
        let lines = Self.steps(translation.y, per: stepHeight)
        let step = CursorPanStep(
            columns: columns - emittedColumns,
            lines: lines - emittedLines
        )
        emittedColumns = columns
        emittedLines = lines
        return step
    }

    private static func steps(_ travel: CGFloat, per step: CGFloat) -> Int {
        Int((travel / step).rounded(.towardZero))
    }
}
#endif
