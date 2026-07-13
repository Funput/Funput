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

    @Test("Preview renders shifted printable label and rejects controls")
    func previewContent() {
        let preview = KeyboardKeyPreviewView()
        var presentation = KeyboardPresentation()
        presentation.shiftState = .uppercase
        let source = CGRect(x: 20, y: 100, width: 36, height: 44)
        let bounds = CGRect(x: 0, y: 0, width: 390, height: 304)

        preview.show(
            key: key("a"),
            sourceFrame: source,
            presentation: presentation,
            traits: UITraitCollection(userInterfaceStyle: .light),
            containerBounds: bounds
        )

        #expect(!preview.isHidden)
        #expect(labels(in: preview).contains("A"))
        #expect(bounds.contains(preview.frame))

        preview.show(
            key: KeySpec(id: "shift", label: "", role: .shift),
            sourceFrame: source,
            presentation: presentation,
            traits: UITraitCollection(),
            containerBounds: bounds
        )
        #expect(preview.isHidden)
    }

    @Test("Secure presentation suppresses key previews")
    func securePreview() {
        var previewCount = 0
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { _ in },
            onPreview: { key, _ in
                if key != nil { previewCount += 1 }
            }
        )
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        presentation.showsKeyPreviews = false

        controller.handle(
            event(key("a"), .pressed),
            sourceFrame: .zero,
            presentation: presentation
        )

        #expect(previewCount == 0)
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

    private func labels(in view: UIView) -> [String] {
        view.subviews.flatMap { child in
            let own = (child as? UILabel)?.text.map { [$0] } ?? []
            return own + labels(in: child)
        }
    }
}
#endif
