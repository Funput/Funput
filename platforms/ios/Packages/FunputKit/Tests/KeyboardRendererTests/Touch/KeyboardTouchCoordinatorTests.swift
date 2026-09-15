#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

@MainActor
struct KeyboardTouchCoordinatorTests {
    @Test("Touch pipeline commits a release and settles ownership")
    func commitContact() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(x: 10, at: 0)
        fixture.clock.now = 0.1
        fixture.end(x: 10, at: 0.1)

        #expect(fixture.output.map(\.key.id) == ["a"])
        #expect(fixture.coordinator.metrics.committedContacts == 1)
        #expect(fixture.coordinator.metrics.releaseCommitted == 1)
        #expect(
            fixture.coordinator.metrics.maximumCaptureToCommitLatencyMilliseconds == 100
        )
        #expect(fixture.coordinator.pendingContactCount == 0)
    }

    @Test("Long holds release while system cancellation never commits")
    func durationAndCancellation() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(x: 10, at: 0)
        fixture.clock.now = 0.301
        fixture.end(x: 10, at: 0.301)
        #expect(fixture.output.map(\.phase) == [.released])

        fixture.begin(id: 2, x: 10, at: 1)
        fixture.coordinator.consume(fixture.sample(.cancelled, id: 2, x: 10, at: 1.1))
        fixture.coordinator.finishUIKitContact(2)

        #expect(fixture.output.last?.phase == .cancelled)
        #expect(fixture.coordinator.metrics.systemCancelled == 1)
        #expect(fixture.coordinator.pendingContactCount == 0)
    }

    @Test("Production metrics observe, settle, and reset directly")
    func directMetrics() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        var observations: [KeyboardTouchMetrics] = []
        fixture.coordinator.observe { observations.append($0) }
        // Both contacts report the same timestamp, which is what a tie looks like.
        fixture.begin(id: 1, x: 10, at: 1)
        fixture.begin(id: 2, x: 10, at: 1)
        fixture.coordinator.recordUnknownCaptureCallback()

        #expect(fixture.coordinator.metrics.capturedContacts == 2)
        #expect(fixture.coordinator.metrics.timestampTieContacts == 2)
        #expect(fixture.coordinator.metrics.maximumConcurrentContacts == 2)
        #expect(fixture.coordinator.metrics.captureUnknownCallback == 1)
        #expect(!observations.isEmpty)

        fixture.coordinator.reset()
        // Neither finger lifted, so the reset drops both presses. That is precisely what
        // `contactsAbandoned` reports, and it survives the counter wipe on purpose — the
        // reset is what produces the evidence.
        #expect(fixture.coordinator.metrics.contactsAbandoned == 2)
        #expect(fixture.coordinator.metrics.capturedContacts == 0)
        #expect(fixture.coordinator.metrics.captureUnknownCallback == 0)
        #expect(fixture.coordinator.activeContactCount == 0)
        #expect(fixture.coordinator.pendingContactCount == 0)
    }

    @Test("Distinct timestamps are not counted as ties")
    func distinctTimestampsAreNotTies() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(id: 1, x: 10, at: 1)
        fixture.begin(id: 2, x: 60, at: 1.01)

        #expect(fixture.coordinator.metrics.capturedContacts == 2)
        #expect(fixture.coordinator.metrics.timestampTieContacts == 0)
    }

    /// The whole path, not just the resolver: `a` spans x 0...45 and `b` starts at 55, so a
    /// press at 40 lands on `a` while a lift at 52 is over `b` — inside the 16pt tap slop, and
    /// the press types `b`, because the stock keyboard commits whatever is under the finger at lift.
    @Test("A press that drifts onto the next key types the key under the lift")
    func driftCommitsTheKeyUnderTheLift() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(x: 40, at: 0)
        fixture.coordinator.consume(fixture.sample(.moved, id: 1, x: 52, at: 0.04))
        fixture.end(x: 52, at: 0.05)

        #expect(fixture.output.map(\.key.id) == ["b"])
        #expect(fixture.coordinator.metrics.recoveredTapSlop == 0)
    }

    /// Sliding the whole way across is how a press is corrected on iOS.
    @Test("A press that slides across to the neighbour types the neighbour")
    func longSlideCommitsTheNeighbour() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(x: 22, at: 0)
        fixture.end(x: 77, at: 0.05)

        #expect(fixture.output.map(\.key.id) == ["b"])
        #expect(fixture.coordinator.metrics.recoveredTapSlop == 1)
    }
}
#endif
