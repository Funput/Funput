#if os(iOS) && canImport(FunputCore)
@testable import KeyboardInput
import Testing

@MainActor
struct SpaceTapTrackerTests {
    @Test("Two spaces inside the window are a double tap")
    func withinWindow() {
        var now: Double = 0
        var tracker = SpaceTapTracker(doubleTapInterval: 0.3) { now }
        let first = tracker.registerSpace()
        now = 0.2
        let second = tracker.registerSpace()
        #expect(!first)
        #expect(second)
    }

    @Test("Two spaces outside the window are two spaces")
    func outsideWindow() {
        var now: Double = 0
        var tracker = SpaceTapTracker(doubleTapInterval: 0.3) { now }
        _ = tracker.registerSpace()
        now = 0.4
        let second = tracker.registerSpace()
        #expect(!second)
    }

    @Test("A firing tap is consumed, so three spaces punctuate once")
    func consumesSequence() {
        var now: Double = 0
        var tracker = SpaceTapTracker(doubleTapInterval: 0.3) { now }
        _ = tracker.registerSpace()
        now = 0.1
        let second = tracker.registerSpace()
        now = 0.2
        let third = tracker.registerSpace()
        #expect(second)
        #expect(!third)
    }

    @Test("Another key between the spaces breaks the sequence")
    func resetBreaksSequence() {
        var now: Double = 0
        var tracker = SpaceTapTracker(doubleTapInterval: 0.3) { now }
        _ = tracker.registerSpace()
        tracker.reset()
        now = 0.1
        let second = tracker.registerSpace()
        #expect(!second)
    }
}
#endif
