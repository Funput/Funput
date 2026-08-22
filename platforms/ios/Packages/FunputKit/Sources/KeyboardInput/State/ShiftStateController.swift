import Foundation
import KeyboardLayout

@MainActor
struct ShiftStateController {
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

    mutating func toggle(from state: ShiftState) -> ShiftState {
        if state == .capsLocked {
            resetTapSequence()
            return .lowercase
        }

        let tapTime = clock()
        // Caps Lock is a double tap from either case. Gating on `.uppercase`
        // only locked when the first tap had come from lowercase — a sentence
        // start that already had Shift armed just toggled twice.
        if let lastTapTime, tapTime - lastTapTime <= doubleTapInterval {
            resetTapSequence()
            return .capsLocked
        }

        lastTapTime = tapTime
        return state == .lowercase ? .uppercase : .lowercase
    }

    mutating func resetTapSequence() {
        lastTapTime = nil
    }
}
