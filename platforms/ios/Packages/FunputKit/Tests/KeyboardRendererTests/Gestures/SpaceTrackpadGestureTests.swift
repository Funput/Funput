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
    /// The old space-repeat delay. Nothing schedules it any more; the tests keep the number to
    /// prove a hold that long no longer costs the user the gesture.
    private static let pastOldRepeat = 0.7

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

    @Test("Holding well past the old repeat delay still starts the trackpad")
    func aLongHoldStillStartsTheTrackpad() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        // Nothing is scheduled here any more, which is the point: the spacebar no longer races
        // the hold by typing spaces while the finger waits.
        subject.scheduler.fire(after: Self.pastOldRepeat)
        subject.move(to: 145)

        #expect(subject.claims == [.trackpad])
        #expect(subject.phases == [.pressed, .cursorMoved(offset: 2)])
    }

    @Test("The spacebar does not repeat while smart gestures are on")
    func spaceDoesNotRepeatWhileArmed() {
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.controller.endTouch(token: 1)

        // No `.repeated`, and no claim: the press stays with the touch pipeline, which commits
        // the single space on release the way an ordinary tap does.
        #expect(subject.phases == [.pressed])
        #expect(subject.claims.isEmpty)
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
