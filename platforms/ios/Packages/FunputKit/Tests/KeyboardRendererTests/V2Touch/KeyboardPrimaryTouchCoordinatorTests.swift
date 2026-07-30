#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import Foundation
import KeyboardLayout
import KeyboardTouchCore
import Testing

@MainActor
struct KeyboardPrimaryTouchCoordinatorTests {
    @Test("V2 commits a release and settles ownership")
    func v2Commit() {
        var output: [KeyboardKeyEvent] = []
        let clock = TestNow()
        let coordinator = KeyboardPrimaryTouchCoordinator(
            clock: { clock.value },
            onEvent: { output.append($0) }
        )
        coordinator.updateGeometry(geometry())
        coordinator.consume(sample(1, .began, 0, x: 10))
        clock.value = 0.1
        coordinator.consume(sample(1, .ended, 0.1, x: 10))
        coordinator.finishUIKitContact(1)

        #expect(output.map(\.key.id) == ["a"])
        #expect(coordinator.metrics.primaryCommitted == 1)
        #expect(coordinator.metrics.releaseCommitted == 1)
        #expect(coordinator.metrics.maximumCaptureToCommitLatencyMilliseconds == 100)
        #expect(coordinator.pendingContactCount == 0)
    }

    @Test("Long holds release while system cancellation never commits")
    func durationAndCancellation() {
        var output: [KeyboardKeyEvent] = []
        let clock = TestNow()
        let coordinator = KeyboardPrimaryTouchCoordinator(
            clock: { clock.value },
            onEvent: { output.append($0) }
        )
        coordinator.updateGeometry(geometry())
        coordinator.consume(sample(1, .began, 0, x: 10))
        clock.value = 0.301
        coordinator.consume(sample(1, .ended, 0.301, x: 10))
        coordinator.finishUIKitContact(1)
        #expect(output.map(\.phase) == [.released])

        coordinator.consume(sample(2, .began, 1, x: 10))
        coordinator.consume(sample(2, .cancelled, 1.1, x: 10))
        coordinator.finishUIKitContact(2)
        #expect(output.last?.phase == .cancelled)
        #expect(coordinator.metrics.primarySystemCancelled == 1)
        #expect(coordinator.pendingContactCount == 0)
    }

    private func geometry() -> ResolvedKeyboard {
        ResolvedKeyboard(
            size: .init(width: 100, height: 50),
            toolbarFrame: nil,
            rows: [[ResolvedKey(
                spec: key("a"),
                frame: .init(x: 0, y: 0, width: 45, height: 50)
            )]]
        )
    }

    private func key(_ id: String) -> KeySpec {
        KeySpec(id: id, label: id, role: .character)
    }

    private func sample(
        _ id: UInt64,
        _ phase: ContactPhase,
        _ timestamp: TimeInterval,
        x: CGFloat
    ) -> ContactSample {
        ContactSample(
            id: .init(rawValue: id),
            phase: phase,
            timestamp: timestamp,
            location: .init(x: x, y: 20),
            previousLocation: .init(x: x, y: 20)
        )
    }
}

@MainActor
private final class TestNow {
    var value = TimeInterval(0)
}
#endif
