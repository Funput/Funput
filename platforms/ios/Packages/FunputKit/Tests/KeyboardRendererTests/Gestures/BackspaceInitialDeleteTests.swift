#if canImport(UIKit)
@testable import KeyboardRenderer
import CoreGraphics
import KeyboardLayout
import Testing

@MainActor
struct BackspaceInitialDeleteTests {
    private static let backspace = KeySpec(id: "backspace", label: "", role: .backspace)
    private static let space = KeySpec(id: "space", label: " ", role: .space)

    @Test("A stationary press deletes once before the repeat timer")
    func stationaryPressDeletesOnce() {
        let subject = GestureTestSubject(key: Self.backspace)
        subject.hapticFeedback = true
        subject.begin()

        #expect(subject.phases == [.pressed, .repeated])
        #expect(subject.claims == [.repeatKey])
        #expect(subject.haptics.performed == [.delete])

        subject.controller.endTouch(token: 1)
        #expect(subject.phases == [.pressed, .repeated, .cancelled])
    }

    @Test("Holding past the delay deletes again without a third delete on release")
    func holdRepeatsThenReleaseStops() {
        let subject = GestureTestSubject(key: Self.backspace)
        subject.hapticFeedback = true
        subject.begin()
        subject.scheduler.fire(after: 0.4)

        #expect(subject.phases == [.pressed, .repeated, .repeated])
        #expect(subject.haptics.performed == [.delete, .deleteRepeat])

        subject.controller.endTouch(token: 1)
        #expect(subject.phases == [.pressed, .repeated, .repeated, .cancelled])
    }

    @Test("A short rub does not delete a second character")
    func shortRubDeletesOnce() {
        let subject = GestureTestSubject(key: Self.backspace)
        subject.begin()
        subject.move(to: 100)
        subject.controller.endTouch(token: 1)

        #expect(subject.claims == [.repeatKey, .wordDelete])
        #expect(subject.phases == [.pressed, .repeated, .cancelled])
    }

    @Test("Space does not delete on contact")
    func spaceWaitsForRelease() {
        let armed = GestureTestSubject(key: Self.space)
        armed.begin()
        #expect(armed.phases == [.pressed])
        #expect(armed.claims.isEmpty)

        let plain = GestureTestSubject(key: Self.space)
        plain.smartGestures = false
        plain.begin()
        #expect(plain.phases == [.pressed])
        #expect(plain.claims.isEmpty)
    }

    @Test("The first delete is claimed only after the pipeline owns the contact")
    func commitFollowsPipelineBegan() {
        let fixture = KeyboardTouchFixture.singleKey(Self.backspace)
        let scheduler = TestGestureScheduler()
        var delivered: [KeyboardKeyEvent.Phase] = []
        var claimed = false
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { _ in },
            onContactEvent: { token, event in
                if let output = fixture.coordinator.handleInteraction(
                    token: token,
                    event: event
                ) {
                    delivered.append(output.phase)
                }
            },
            onClaimGesture: { token, kind in
                claimed = fixture.coordinator.claim(token: token, kind: kind)
                return claimed
            },
            onPreview: { _, _ in },
            repeatScheduler: scheduler.schedule
        )

        controller.beginTouch(
            token: 1,
            key: fixture.key,
            point: CGPoint(x: 25, y: 25),
            sourceFrame: CGRect(x: 0, y: 0, width: 50, height: 50),
            containerBounds: CGRect(x: 0, y: 0, width: 50, height: 50),
            presentation: KeyboardPresentation()
        )
        fixture.begin()
        controller.commitInitialBackspaces()

        #expect(claimed)
        #expect(delivered == [.pressed, .repeated])

        fixture.coordinator.consume(fixture.sample(.ended, at: 0.1))
        controller.endTouch(token: 1)
        fixture.coordinator.finishUIKitContact(1)

        #expect(delivered == [.pressed, .repeated])
        #expect(fixture.output.isEmpty)
    }
}
#endif
