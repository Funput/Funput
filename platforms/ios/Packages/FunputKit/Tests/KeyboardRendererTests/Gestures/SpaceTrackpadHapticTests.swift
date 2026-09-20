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
        subject.begin()
        #expect(!subject.haptics.performed.contains(.modeChange))

        subject.scheduler.fire(after: Self.hold)

        // Straight after the press's own `.space`, and before the first sideways pixel: by
        // the time the caret moves, the user can see the answer and the buzz is late.
        #expect(subject.haptics.performed == [.space, .modeChange])
        subject.move(to: 145)
        #expect(subject.haptics.performed.filter { $0 == .modeChange }.count == 1)
    }

    @Test("The mode buzz does not follow the typing setting")
    func modeBuzzOutlivesTheTypingSetting() {
        // Haptics off, the way the setting ships.
        let subject = subject()
        subject.begin()
        subject.scheduler.fire(after: Self.hold)
        subject.move(to: 145)

        // The press and every caret step stay silent; the one signal a gesture with no
        // visible state depends on still arrives.
        #expect(subject.haptics.performed == [.modeChange])
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
