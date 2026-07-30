#if canImport(UIKit)
@testable import KeyboardRenderer
import Foundation
import KeyboardLayout
import KeyboardTouchCore
import Testing

@MainActor
struct KeyboardV2GestureTests {
    @Test("Repeat uses its own lane and suppresses base release")
    func repeatLane() {
        let fixture = Fixture(key: key("delete", .backspace))
        fixture.begin()
        fixture.coordinator.claim(token: 1, kind: .repeatKey)
        let repeated = fixture.event(.repeated)
        #expect(fixture.coordinator.handleInteraction(token: 1, event: repeated) != nil)
        fixture.coordinator.consume(fixture.sample(.ended, at: 0.5))
        _ = fixture.coordinator.handleInteraction(
            token: 1,
            event: fixture.event(.cancelled)
        )
        fixture.coordinator.finishUIKitContact(1)
        #expect(fixture.output.map(\.phase) == [])
        #expect(fixture.coordinator.metrics.repeatEmitted == 1)
        #expect(fixture.coordinator.pendingContactCount == 0)
    }

    @Test("Alternate and swipe are terminal V2 actions")
    func alternateAndSwipe() {
        let alternate = Fixture(key: key("a", .character))
        alternate.begin()
        alternate.coordinator.claim(token: 1, kind: .alternate)
        _ = alternate.coordinator.handleInteraction(
            token: 1,
            event: alternate.event(.alternateSelected(.init(text: "á")))
        )
        alternate.coordinator.finishUIKitContact(1)
        #expect(alternate.output.map(\.phase) == [.alternateSelected(.init(text: "á"))])
        #expect(alternate.coordinator.metrics.alternateCommitted == 1)

        let swipe = Fixture(key: key("space", .space, swipe: .toggleLanguage))
        swipe.begin()
        swipe.coordinator.claim(token: 1, kind: .swipe)
        _ = swipe.coordinator.handleInteraction(
            token: 1,
            event: swipe.event(.swiped(.toggleLanguage))
        )
        swipe.coordinator.finishUIKitContact(1)
        #expect(swipe.output.map(\.phase) == [.swiped(.toggleLanguage)])
        #expect(swipe.coordinator.metrics.swipeCommitted == 1)
    }

    @Test("Every control role releases exactly once")
    func controls() {
        let roles: [KeyRole] = [
            .shift, .backspace, .symbols, .moreSymbols, .letters,
            .inputMethod, .systemInputMode, .enter, .emoji,
        ]
        for role in roles {
            let fixture = Fixture(key: key(role.rawValue, role))
            fixture.begin()
            fixture.coordinator.consume(fixture.sample(.ended, at: 0.1))
            fixture.coordinator.finishUIKitContact(1)
            #expect(fixture.output.map(\.phase) == [.released])
            #expect(fixture.coordinator.metrics.v2Committed == 1)
        }
    }

    private func key(
        _ id: String,
        _ role: KeyRole,
        swipe: KeySwipeAction? = nil
    ) -> KeySpec {
        KeySpec(id: id, label: id, role: role, horizontalSwipeAction: swipe)
    }
}

@MainActor
private final class Fixture {
    private final class EventBox {
        var values: [KeyboardKeyEvent] = []
    }

    let key: KeySpec
    let coordinator: KeyboardV2TouchCoordinator
    private let box = EventBox()
    var output: [KeyboardKeyEvent] { box.values }

    init(key: KeySpec) {
        self.key = key
        let eventBox = box
        coordinator = KeyboardV2TouchCoordinator { eventBox.values.append($0) }
        coordinator.updateGeometry(
            ResolvedKeyboard(
                size: .init(width: 50, height: 50),
                toolbarFrame: nil,
                rows: [[ResolvedKey(
                    spec: key,
                    frame: .init(x: 0, y: 0, width: 50, height: 50)
                )]]
            )
        )
    }

    func begin() {
        coordinator.consume(sample(.began, at: 0))
    }

    func sample(_ phase: ContactPhase, at timestamp: TimeInterval) -> ContactSample {
        ContactSample(
            id: .init(rawValue: 1),
            phase: phase,
            timestamp: timestamp,
            location: .init(x: 25, y: 25),
            previousLocation: .init(x: 25, y: 25)
        )
    }

    func event(_ phase: KeyboardKeyEvent.Phase) -> KeyboardKeyEvent {
        KeyboardKeyEvent(key: key, phase: phase)
    }
}
#endif
