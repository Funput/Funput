#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing

@MainActor
struct KeyboardTouchResetTests {
    @Test("A layout swap commits presses whose finger already lifted")
    func flushOnLayoutChange() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(id: 1, x: 10, at: 0)
        fixture.begin(id: 2, x: 60, at: 0.01)
        fixture.end(id: 2, x: 60, at: 0.02)
        // Contact 2 sits behind contact 1, whose finger is still down.
        #expect(fixture.output.isEmpty)

        fixture.coordinator.reset(flushingResolvedPresses: true)

        #expect(fixture.output.map(\.key.id) == ["b"])
        #expect(fixture.output.map(\.phase) == [.released])
        #expect(fixture.coordinator.metrics.flushedOnLayoutChange == 1)
    }

    @Test("A teardown without flushing writes nothing to the document")
    func resetWithoutFlush() {
        let fixture = KeyboardTouchFixture.adjacentKeys()
        fixture.begin(id: 1, x: 10, at: 0)
        fixture.begin(id: 2, x: 60, at: 0.01)
        fixture.end(id: 2, x: 60, at: 0.02)

        fixture.coordinator.reset()

        #expect(fixture.output.isEmpty)
        #expect(fixture.coordinator.metrics.flushedOnLayoutChange == 0)
    }
}
#endif
