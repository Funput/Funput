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
}
#endif
