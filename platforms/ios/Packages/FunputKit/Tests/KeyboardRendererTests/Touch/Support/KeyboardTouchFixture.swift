#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import Foundation
import KeyboardLayout
import KeyboardTouchCore

/// One coordinator wired to a scripted geometry, shared by every touch test in this folder.
///
/// Each test used to grow its own near-identical fixture; keeping one here means a change to
/// how the coordinator is built lands in a single place.
@MainActor
final class KeyboardTouchFixture {
    private final class EventBox {
        var values: [KeyboardKeyEvent] = []
    }

    /// Test-controlled clock, so latency assertions do not depend on wall time.
    @MainActor
    final class Clock {
        var now = TimeInterval(0)
    }

    let keys: [KeySpec]
    let clock = Clock()
    let coordinator: KeyboardTouchCoordinator
    private let box = EventBox()
    var output: [KeyboardKeyEvent] { box.values }
    var key: KeySpec { keys[0] }

    /// One key filling a 50x50 surface; taps land at its centre, (25, 25).
    static func singleKey(_ spec: KeySpec) -> KeyboardTouchFixture {
        KeyboardTouchFixture(
            size: CGSize(width: 50, height: 50),
            resolved: [ResolvedKey(
                spec: spec,
                frame: CGRect(x: 0, y: 0, width: 50, height: 50)
            )]
        )
    }

    /// Two character keys side by side on a 100x50 surface: "a" around x = 10, "b" around 60.
    static func adjacentKeys() -> KeyboardTouchFixture {
        KeyboardTouchFixture(
            size: CGSize(width: 100, height: 50),
            resolved: [
                ResolvedKey(
                    spec: KeySpec(id: "a", label: "a", role: .character),
                    frame: CGRect(x: 0, y: 0, width: 45, height: 50)
                ),
                ResolvedKey(
                    spec: KeySpec(id: "b", label: "b", role: .character),
                    frame: CGRect(x: 55, y: 0, width: 45, height: 50)
                ),
            ]
        )
    }

    private init(size: CGSize, resolved: [ResolvedKey]) {
        keys = resolved.map(\.spec)
        let eventBox = box
        let testClock = clock
        coordinator = KeyboardTouchCoordinator(clock: { testClock.now }) {
            eventBox.values.append($0)
        }
        coordinator.updateGeometry(
            ResolvedKeyboard(size: size, toolbarFrame: nil, rows: [resolved])
        )
    }

    func begin(id: UInt64 = 1, x: CGFloat = 25, at timestamp: TimeInterval = 0) {
        coordinator.consume(sample(.began, id: id, x: x, at: timestamp))
    }

    /// Ends a contact the way UIKit does: the pipeline sees the sample, then the capture layer
    /// reports the touch finished.
    func end(id: UInt64 = 1, x: CGFloat = 25, at timestamp: TimeInterval) {
        coordinator.consume(sample(.ended, id: id, x: x, at: timestamp))
        coordinator.finishUIKitContact(id)
    }

    /// The phase leads and everything else is labelled: a defaulted positional `id` in front
    /// would silently swallow the phase argument.
    func sample(
        _ phase: ContactPhase,
        id: UInt64 = 1,
        x: CGFloat = 25,
        at timestamp: TimeInterval
    ) -> ContactSample {
        ContactSample(
            id: .init(rawValue: id),
            phase: phase,
            timestamp: timestamp,
            location: .init(x: x, y: 25),
            previousLocation: .init(x: x, y: 25)
        )
    }

    func event(_ phase: KeyboardKeyEvent.Phase) -> KeyboardKeyEvent {
        KeyboardKeyEvent(key: key, phase: phase)
    }
}
#endif
