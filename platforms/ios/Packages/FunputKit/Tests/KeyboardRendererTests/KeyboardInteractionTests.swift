#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct KeyboardInteractionTests {
    @Test("Overlapping presses are tracked for rollover instead of cancelled")
    func rolloverKeepsBothKeys() {
        var events: [KeyboardKeyEvent] = []
        var previews: [String?] = []
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { events.append($0) },
            onPreview: { key, _ in previews.append(key?.id) }
        )
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        let first = key("a")
        let second = key("b")

        // Press "a", press "b" before "a" is released, then release both.
        controller.handle(event(first, .pressed), sourceFrame: .zero, presentation: presentation)
        controller.handle(event(second, .pressed), sourceFrame: .zero, presentation: presentation)
        controller.handle(event(first, .released), sourceFrame: .zero, presentation: presentation)
        controller.handle(event(second, .released), sourceFrame: .zero, presentation: presentation)

        // No key is dropped: both commit on their own release.
        #expect(events.map(\.phase) == [.pressed, .pressed, .released, .released])
        #expect(events.map(\.key.id) == ["key-a", "key-b", "key-a", "key-b"])
        #expect(controller.activeKey == nil)
        #expect(previews == ["key-a", "key-b", nil])
    }

    @Test("Sustained rollover never cancels or drops a release")
    func sustainedRollover() {
        var events: [KeyboardKeyEvent] = []
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { events.append($0) },
            onPreview: { _, _ in }
        )
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        presentation.showsKeyPreviews = false

        for index in 0..<1_000 {
            let first = key("a-\(index)")
            let second = key("b-\(index)")
            controller.handle(
                event(first, .pressed),
                sourceFrame: nil,
                presentation: presentation
            )
            controller.handle(
                event(second, .pressed),
                sourceFrame: nil,
                presentation: presentation
            )
            if index.isMultiple(of: 2) {
                controller.handle(
                    event(first, .released),
                    sourceFrame: nil,
                    presentation: presentation
                )
                controller.handle(
                    event(second, .released),
                    sourceFrame: nil,
                    presentation: presentation
                )
            } else {
                controller.handle(
                    event(second, .released),
                    sourceFrame: nil,
                    presentation: presentation
                )
                controller.handle(
                    event(first, .released),
                    sourceFrame: nil,
                    presentation: presentation
                )
            }
        }

        #expect(events.filter { $0.phase == .released }.count == 2_000)
        #expect(!events.contains { $0.phase == .cancelled })
        #expect(controller.activeKey == nil)
    }

    @Test("Backspace repeat emits ticks and suppresses release")
    func backspaceRepeatIntegration() {
        let scheduler = TestRepeatScheduler()
        var events: [KeyboardKeyEvent] = []
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { events.append($0) },
            onPreview: { _, _ in },
            repeatScheduler: scheduler.schedule
        )
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        let backspace = KeySpec(id: "backspace", label: "", role: .backspace)

        controller.handle(
            event(backspace, .pressed),
            sourceFrame: nil,
            presentation: presentation
        )
        scheduler.runNext()
        controller.handle(
            event(backspace, .released),
            sourceFrame: nil,
            presentation: presentation
        )

        #expect(events.map(\.phase) == [.pressed, .repeated])
        #expect(controller.activeKey == nil)
    }

    private func key(_ label: String) -> KeySpec {
        KeySpec(
            id: "key-\(label)",
            label: label,
            role: .character,
            shiftedLabel: label.uppercased()
        )
    }

    private func event(_ key: KeySpec, _ phase: KeyboardKeyEvent.Phase) -> KeyboardKeyEvent {
        KeyboardKeyEvent(key: key, phase: phase)
    }
}
#endif
