#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

/// What the caret trackpad tells the finger, for a gesture that shows nothing on screen
/// until the caret already moved.
@MainActor
struct SpaceTrackpadHapticTests {
    @Test("The hold that readies the trackpad is what the finger feels")
    func armingReportsItself() {
        let subject = subject()
        subject.hapticFeedback = true
        subject.hapticStrength = .strong
        subject.begin()
        #expect(subject.haptics.performed == [.strong])

        subject.scheduler.fire(after: Self.hold)

        #expect(subject.haptics.performed == [.strong, .strong])
        subject.move(to: 145)
        #expect(subject.haptics.performed == [.strong, .strong, .strong])
    }

    @Test("Off silences the press, trackpad hold and caret movement")
    func offSilencesTheTrackpad() {
        // Haptics off, the way the setting ships.
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 145)

        #expect(subject.haptics.performed.isEmpty)
        #expect(subject.claims == [.trackpad])
    }

    private func subject() -> GestureTestSubject {
        GestureTestSubject(
            key: KeySpec(
                id: "space",
                label: "Tiếng Việt",
                role: .space,
                horizontalSwipeAction: .toggleLanguage
            )
        )
    }

    private static let hold = 0.35
}
#endif
