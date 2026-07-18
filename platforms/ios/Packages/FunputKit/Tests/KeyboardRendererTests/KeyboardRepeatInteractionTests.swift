#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

@MainActor
struct KeyboardRepeatInteractionTests {
    @Test("Backspace repeat emits ticks and suppresses release")
    func backspaceRepeatIntegration() {
        let backspace = KeySpec(id: "backspace", label: "", role: .backspace)
        let (controller, scheduler, events, presentation) = makeSubject()

        controller.handle(event(backspace, .pressed), sourceFrame: nil, presentation: presentation)
        scheduler.runNext()
        controller.handle(event(backspace, .released), sourceFrame: nil, presentation: presentation)

        #expect(events.value.map(\.phase) == [.pressed, .repeated])
        #expect(controller.activeKey == nil)
    }

    @Test("Space hold emits repeated spaces and suppresses the release")
    func spaceRepeatIntegration() {
        let space = KeySpec(id: "space", label: "space", role: .space)
        let (controller, scheduler, events, presentation) = makeSubject()

        controller.handle(event(space, .pressed), sourceFrame: nil, presentation: presentation)
        scheduler.runNext()
        scheduler.runNext()
        controller.handle(event(space, .released), sourceFrame: nil, presentation: presentation)

        #expect(events.value.map(\.phase) == [.pressed, .repeated, .repeated])
        #expect(events.value.filter { $0.key.role == .space && $0.phase == .repeated }.count == 2)
        #expect(controller.activeKey == nil)
    }

    private final class EventBox { var value: [KeyboardKeyEvent] = [] }

    private func makeSubject() -> (
        KeyboardSurfaceInteractionController,
        TestRepeatScheduler,
        EventBox,
        KeyboardPresentation
    ) {
        let scheduler = TestRepeatScheduler()
        let events = EventBox()
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { events.value.append($0) },
            onPreview: { _, _ in },
            repeatScheduler: scheduler.schedule
        )
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        return (controller, scheduler, events, presentation)
    }

    private func event(_ key: KeySpec, _ phase: KeyboardKeyEvent.Phase) -> KeyboardKeyEvent {
        KeyboardKeyEvent(key: key, phase: phase)
    }
}
#endif
