#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing

@MainActor
struct KeyboardSpaceRepeatTests {
    @Test("Holding Space repeats and suppresses its release")
    func holdSpace() {
        let (controller, scheduler, events) = makeController()
        let space = key(id: "space", role: .space)

        controller.handle(event(space, .pressed), sourceFrame: nil, presentation: presentation())
        scheduler.runNext()
        controller.handle(event(space, .released), sourceFrame: nil, presentation: presentation())

        #expect(events.value.map(\.phase) == [.pressed, .repeated])
    }

    @Test("Tapping Space still emits exactly one release")
    func tapSpace() {
        let (controller, scheduler, events) = makeController()
        let space = key(id: "space", role: .space)

        controller.handle(event(space, .pressed), sourceFrame: nil, presentation: presentation())
        controller.handle(event(space, .released), sourceFrame: nil, presentation: presentation())
        scheduler.runNext()

        #expect(events.value.map(\.phase) == [.pressed, .released])
    }

    @Test("Swiping Space cancels repeat")
    func swipeCancelsRepeat() {
        let (controller, scheduler, events) = makeController()
        let space = KeySpec(
            id: "space",
            label: "Space",
            role: .space,
            horizontalSwipeAction: .toggleLanguage
        )

        controller.handle(event(space, .pressed), sourceFrame: nil, presentation: presentation())
        controller.handle(
            event(space, .swiped(.toggleLanguage)),
            sourceFrame: nil,
            presentation: presentation()
        )
        scheduler.runNext()
        controller.handle(event(space, .released), sourceFrame: nil, presentation: presentation())

        #expect(events.value.map(\.phase) == [.pressed, .swiped(.toggleLanguage)])
    }

    @Test("Repeated Space waits behind an earlier press")
    func repeatPreservesPressOrder() {
        let (controller, scheduler, events) = makeController()
        let character = key(id: "a", role: .character)
        let space = key(id: "space", role: .space)

        controller.handle(event(character, .pressed), sourceFrame: nil, presentation: presentation())
        controller.handle(event(space, .pressed), sourceFrame: nil, presentation: presentation())
        scheduler.runNext()
        #expect(!events.value.contains { $0.phase == .repeated })
        controller.handle(event(space, .released), sourceFrame: nil, presentation: presentation())
        controller.handle(event(character, .released), sourceFrame: nil, presentation: presentation())

        let mutations = events.value.filter { $0.phase == .released || $0.phase == .repeated }
        #expect(mutations.map(\.key.id) == [character.id, space.id])
        #expect(mutations.map(\.phase) == [.released, .repeated])
    }

    private final class EventBox {
        var value: [KeyboardKeyEvent] = []
    }

    private func makeController() -> (
        KeyboardSurfaceInteractionController,
        TestRepeatScheduler,
        EventBox
    ) {
        let scheduler = TestRepeatScheduler()
        let events = EventBox()
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { events.value.append($0) },
            onPreview: { _, _ in },
            repeatScheduler: scheduler.schedule
        )
        return (controller, scheduler, events)
    }

    private func presentation() -> KeyboardPresentation {
        var value = KeyboardPresentation()
        value.isHapticFeedbackEnabled = false
        return value
    }

    private func key(id: String, role: KeyRole) -> KeySpec {
        KeySpec(id: id, label: id, role: role)
    }

    private func event(_ key: KeySpec, _ phase: KeyboardKeyEvent.Phase) -> KeyboardKeyEvent {
        KeyboardKeyEvent(key: key, phase: phase)
    }
}
#endif
