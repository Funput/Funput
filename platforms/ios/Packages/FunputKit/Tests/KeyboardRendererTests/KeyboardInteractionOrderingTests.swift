#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

@MainActor
struct KeyboardInteractionOrderingTests {
    @Test("Reverse touch-up order still commits in touch-down order")
    func reverseReleaseOrder() {
        let (controller, events) = makeController()
        let presentation = presentation()
        let first = key("a"), second = key("b")

        controller.handle(event(first, .pressed), sourceFrame: nil, presentation: presentation)
        controller.handle(event(second, .pressed), sourceFrame: nil, presentation: presentation)
        controller.handle(event(second, .released), sourceFrame: nil, presentation: presentation)
        #expect(events.value.filter { $0.phase == .released }.isEmpty)
        controller.handle(event(first, .released), sourceFrame: nil, presentation: presentation)

        let releases = events.value.filter { $0.phase == .released }.map(\.key.id)
        #expect(releases == [first.id, second.id])
    }

    @Test("Cancelling an earlier touch unblocks a later completed key")
    func cancellationUnblocksQueue() {
        let (controller, events) = makeController()
        let presentation = presentation()
        let first = key("a"), second = key("b")

        controller.handle(event(first, .pressed), sourceFrame: nil, presentation: presentation)
        controller.handle(event(second, .pressed), sourceFrame: nil, presentation: presentation)
        controller.handle(event(second, .released), sourceFrame: nil, presentation: presentation)
        controller.handle(event(first, .cancelled), sourceFrame: nil, presentation: presentation)

        #expect(events.value.suffix(2).map(\.phase) == [.cancelled, .released])
        #expect(events.value.suffix(2).map(\.key.id) == [first.id, second.id])
        #expect(controller.activeKey == nil)
    }

    @Test("Backspace repeats wait behind earlier presses")
    func backspaceRepeatPreservesPressOrder() {
        let scheduler = TestRepeatScheduler()
        var events: [KeyboardKeyEvent] = []
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { events.append($0) },
            onPreview: { _, _ in },
            repeatScheduler: scheduler.schedule
        )
        let presentation = presentation()
        let first = key("a")
        let backspace = KeySpec(id: "backspace", label: "", role: .backspace)

        controller.handle(event(first, .pressed), sourceFrame: nil, presentation: presentation)
        controller.handle(event(backspace, .pressed), sourceFrame: nil, presentation: presentation)
        scheduler.runNext()
        #expect(!events.contains { $0.phase == .repeated })
        controller.handle(event(first, .released), sourceFrame: nil, presentation: presentation)
        controller.handle(event(backspace, .released), sourceFrame: nil, presentation: presentation)

        let mutations = events.filter { $0.phase == .released || $0.phase == .repeated }
        #expect(mutations.map(\.phase) == [.released, .repeated])
        #expect(mutations.map(\.key.id) == [first.id, backspace.id])
    }

    private final class EventBox {
        var value: [KeyboardKeyEvent] = []
    }

    private func makeController() -> (KeyboardSurfaceInteractionController, EventBox) {
        let events = EventBox()
        return (
            KeyboardSurfaceInteractionController(
                onEvent: { events.value.append($0) },
                onPreview: { _, _ in }
            ),
            events
        )
    }

    private func presentation() -> KeyboardPresentation {
        var value = KeyboardPresentation()
        value.isHapticFeedbackEnabled = false
        return value
    }

    private func key(_ label: String) -> KeySpec {
        KeySpec(id: "key-\(label)", label: label, role: .character)
    }

    private func event(_ key: KeySpec, _ phase: KeyboardKeyEvent.Phase) -> KeyboardKeyEvent {
        KeyboardKeyEvent(key: key, phase: phase)
    }
}
#endif
