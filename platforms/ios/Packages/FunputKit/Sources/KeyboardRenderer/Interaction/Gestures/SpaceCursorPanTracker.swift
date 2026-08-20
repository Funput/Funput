#if canImport(UIKit)
import CoreGraphics

/// Converts horizontal travel on the spacebar into whole-character caret steps.
///
/// Keeps the sub-step remainder so a slow drag advances exactly once per `stepWidth`
/// instead of drifting, and so reversing direction costs nothing.
struct SpaceCursorPanTracker {
    private let stepWidth: CGFloat
    private var emittedSteps = 0

    init(stepWidth: CGFloat = 10) {
        precondition(stepWidth > 0)
        self.stepWidth = stepWidth
    }

    /// - Parameter translationX: total travel since the pan began, not since the last call.
    /// - Returns: signed character offset to apply now; zero when the finger has not
    ///   crossed into a new step.
    mutating func update(translationX: CGFloat) -> Int {
        let steps = Int((translationX / stepWidth).rounded(.towardZero))
        let delta = steps - emittedSteps
        guard delta != 0 else { return 0 }
        emittedSteps = steps
        return delta
    }
}
#endif
