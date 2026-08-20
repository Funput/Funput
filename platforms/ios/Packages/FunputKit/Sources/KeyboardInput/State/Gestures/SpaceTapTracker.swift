import Foundation

/// Recognizes two space presses in quick succession.
///
/// Deliberately shaped like ``ShiftStateController``: a value type over an injected
/// clock, so the double-tap window is testable without waiting on wall time.
@MainActor
struct SpaceTapTracker {
    private let doubleTapInterval: TimeInterval
    private let clock: () -> TimeInterval
    private var lastTapTime: TimeInterval?

    init(
        doubleTapInterval: TimeInterval = 0.3,
        clock: @escaping () -> TimeInterval = { ProcessInfo.processInfo.systemUptime }
    ) {
        self.doubleTapInterval = doubleTapInterval
        self.clock = clock
    }

    /// Records a space press and reports whether it completes a double tap.
    ///
    /// A firing press clears the sequence, so three spaces produce one substitution
    /// and a plain space rather than a second one.
    mutating func registerSpace() -> Bool {
        let tapTime = clock()
        if let lastTapTime, tapTime - lastTapTime <= doubleTapInterval {
            reset()
            return true
        }
        lastTapTime = tapTime
        return false
    }

    /// Called for every non-space key: two spaces separated by other input are not a
    /// double tap, however fast they arrive.
    mutating func reset() {
        lastTapTime = nil
    }
}
