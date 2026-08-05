#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing

@MainActor
struct KeyboardTouchResetTests {
    /// A reset is a real ending — the keyboard leaving its window, or a diagnostics session
    /// starting over. Nothing is rescued on the way out: a finger that never lifted did not
    /// finish a press, so it must not reach the document.
    @Test("A teardown writes nothing to the document")
    func resetWritesNothing() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(id: 1, x: 10, at: 0)
        fixture.begin(id: 2, x: 60, at: 0.01)
        fixture.end(id: 2, x: 60, at: 0.02)

        fixture.coordinator.reset()

        #expect(fixture.output.isEmpty)
    }

    /// Contact 2's finger lifted, but its press sat behind contact 1 in the ordering window,
    /// so the teardown drops it. That is a lost keystroke and the counter has to say so — a
    /// silent drop is exactly what made this class of bug so hard to find.
    @Test("A teardown reports the presses it drops")
    func resetReportsAbandonedContacts() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(id: 1, x: 10, at: 0)
        fixture.begin(id: 2, x: 60, at: 0.01)
        fixture.end(id: 2, x: 60, at: 0.02)

        fixture.coordinator.reset()

        #expect(fixture.coordinator.metrics.contactsAbandoned == 2)
    }
}
#endif
