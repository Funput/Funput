#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

/// A press the system takes away is still a press the user made, so it commits —
/// with the exclusions that keep that from inventing input nobody asked for.
@MainActor
struct KeyboardSystemCancellationTests {
    private func makeController(
        _ events: @escaping (KeyboardKeyEvent) -> Void
    ) -> KeyboardSurfaceInteractionController {
        KeyboardSurfaceInteractionController(onEvent: events, onPreview: { _, _ in })
    }

    private var presentation: KeyboardPresentation {
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        return presentation
    }

    private func press(
        _ key: KeySpec,
        on controller: KeyboardSurfaceInteractionController,
        token: KeyboardPressCommitQueue.TouchToken = 1
    ) {
        controller.beginTouch(
            token: token,
            key: key,
            point: .zero,
            sourceFrame: nil,
            presentation: presentation
        )
    }

    @Test("A letter the system cancels is still typed")
    func systemCancellationCommitsALetter() {
        var events: [KeyboardKeyEvent] = []
        let controller = makeController { events.append($0) }
        let letter = KeySpec(id: "key-a", label: "a", role: .character)

        press(letter, on: controller)
        controller.cancelTouch(token: 1, reason: .system)

        #expect(events.map(\.phase) == [.pressed, .released])
        #expect(events.last?.key.id == letter.id)
    }

    @Test("Backspace and Return are not invented on a system cancellation")
    func destructiveKeysStayCancelled() {
        for role in [KeyRole.backspace, .enter] {
            var events: [KeyboardKeyEvent] = []
            let controller = makeController { events.append($0) }
            press(KeySpec(id: "key-\(role)", label: "x", role: role), on: controller)

            controller.cancelTouch(token: 1, reason: .system)

            #expect(events.map(\.phase) == [.pressed, .cancelled], "role: \(role)")
        }
    }

    @Test("A finger that wandered is a gesture, not a press")
    func wanderedTouchStaysCancelled() {
        var events: [KeyboardKeyEvent] = []
        let controller = makeController { events.append($0) }
        let space = KeySpec(id: "key-space", label: " ", role: .space)

        press(space, on: controller)
        // A swipe up from the home indicator starts on the bottom row and ends as
        // a system cancellation; it must not leave a space behind.
        controller.moveTouch(
            token: 1,
            key: space,
            point: CGPoint(x: 0, y: -40),
            sourceFrame: nil,
            presentation: presentation
        )
        controller.cancelTouch(token: 1, reason: .system)

        #expect(events.map(\.phase) == [.pressed, .cancelled])
    }

    @Test("Lifting outside a toolbar control still discards the press")
    func userIntentStaysCancelled() {
        var events: [KeyboardKeyEvent] = []
        let controller = makeController { events.append($0) }
        let emoji = KeySpec(id: "key-emoji", label: "☺", role: .emoji)

        controller.handle(
            KeyboardKeyEvent(key: emoji, phase: .pressed),
            sourceFrame: .zero,
            presentation: presentation
        )
        controller.handle(
            KeyboardKeyEvent(key: emoji, phase: .cancelled),
            sourceFrame: .zero,
            presentation: presentation
        )

        #expect(events.map(\.phase) == [.pressed, .cancelled])
    }

    @Test("Teardown discards pending presses instead of committing them")
    func teardownStaysCancelled() {
        var events: [KeyboardKeyEvent] = []
        let controller = makeController { events.append($0) }
        press(KeySpec(id: "key-a", label: "a", role: .character), on: controller)

        controller.cancelAll()

        #expect(events.map(\.phase) == [.pressed, .cancelled])
    }
}
#endif
