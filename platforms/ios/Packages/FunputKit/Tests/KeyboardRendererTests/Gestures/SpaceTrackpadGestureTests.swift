#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

@MainActor
struct SpaceTrackpadGestureTests {
    private static let spaceKey = KeySpec(
        id: "space",
        label: "Tiếng Việt",
        role: .space,
        horizontalSwipeAction: .toggleLanguage
    )
    private static let hold = 0.35
    private static let smartRepeat = 0.7

    @Test("A quick swipe still toggles the language")
    func quickSwipeTogglesLanguage() {
        let subject = subject()
        subject.begin()
        subject.move(to: 180)

        #expect(subject.phases.contains(.swiped(.toggleLanguage)))
        #expect(!subject.phases.contains { if case .cursorMoved = $0 { true } else { false } })
    }

    @Test("Holding then dragging moves the caret instead")
    func holdThenDragMovesCaret() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 145)

        #expect(subject.claims == [.trackpad])
        #expect(subject.phases == [.pressed, .cursorMoved(offset: 2)])
        #expect(!subject.phases.contains(.swiped(.toggleLanguage)))
    }

    @Test("The caret follows the finger back and forth")
    func trackpadFollowsFinger() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 150)
        subject.move(to: 130)

        #expect(subject.phases == [
            .pressed,
            .cursorMoved(offset: 3),
            .cursorMoved(offset: -2),
        ])
    }

    @Test("Once the space has repeated, dragging no longer starts the trackpad")
    func repeatWinsOverTrackpad() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.scheduler.fire(after: Self.smartRepeat)
        subject.move(to: 145)

        #expect(subject.claims == [.repeatKey])
        #expect(subject.phases == [.pressed, .repeated])
    }

    @Test("A refused claim leaves the press with the touch pipeline")
    func refusedClaimFallsThrough() {
        let subject = subject()
        subject.grantsClaims = false
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 145)

        #expect(subject.phases == [.pressed])
    }

    @Test("With smart gestures off the spacebar behaves exactly as before")
    func disabledKeepsOldBehaviour() {
        let subject = subject()
        subject.smartGestures = false
        subject.begin()
        subject.scheduler.fire(after: 0.4)
        subject.move(to: 145)

        #expect(subject.claims == [.repeatKey])
        #expect(subject.phases == [.pressed, .repeated])
    }

    @Test("Releasing a trackpad drag commits no space")
    func releaseCommitsNothing() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 150)
        subject.controller.endTouch(token: 1)

        #expect(subject.phases.last == .cancelled)
    }

    private func subject() -> GestureTestSubject {
        GestureTestSubject(key: Self.spaceKey)
    }
}
#endif
