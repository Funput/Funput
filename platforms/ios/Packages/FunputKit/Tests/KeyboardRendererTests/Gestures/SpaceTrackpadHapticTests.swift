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
        // Caret steps tick rather than strike, whatever the chosen strength.
        #expect(subject.haptics.performed == [.strong, .strong])
        #expect(subject.haptics.ticks == 1)
    }

    @Test("Off silences the press and caret steps, but not the trackpad hold")
    func modeBuzzOutlivesTheTypingSetting() {
        // Haptics off, the way the setting ships, with a remembered strength.
        let subject = subject()
        subject.hapticStrength = .medium
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 145)

        // The one signal a gesture with no visible state depends on still arrives.
        #expect(subject.haptics.performed == [.medium])
        #expect(subject.haptics.ticks == 0)
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
