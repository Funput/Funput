#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct LanguageSwipeInteractionTests {
    @Test("Language swipe suppresses the Space release")
    func suppressesSpace() {
        var events: [KeyboardKeyEvent] = []
        let controller = KeyboardSurfaceInteractionController(
            onEvent: { events.append($0) },
            onPreview: { _, _ in }
        )
        var presentation = KeyboardPresentation()
        presentation.isHapticFeedbackEnabled = false
        let space = KeySpec(
            id: "space",
            label: "Tiếng Việt",
            role: .space,
            horizontalSwipeAction: .toggleLanguage
        )

        controller.handle(event(space, .pressed), sourceFrame: .zero, presentation: presentation)
        controller.handle(
            event(space, .swiped(.toggleLanguage)),
            sourceFrame: .zero,
            presentation: presentation
        )
        controller.handle(event(space, .released), sourceFrame: .zero, presentation: presentation)

        #expect(events.map(\.phase) == [.pressed, .swiped(.toggleLanguage)])
    }

    @Test("Space exposes a language toggle accessibility action")
    func accessibilityAction() {
        let layout = StandardKeyboardLayouts.letters(.vni)
        let space = layout.rows.flatMap(\.keys).first { $0.role == .space }!
        let control = KeyboardKeyControl(spec: space)
        var presentation = KeyboardPresentation(layout: layout)
        presentation.language = .english

        control.apply(
            presentation: presentation,
            traits: UITraitCollection(userInterfaceStyle: .light)
        )

        #expect(control.accessibilityCustomActions?.map(\.name) == ["Chuyển sang Tiếng Việt"])
    }

    private func event(
        _ key: KeySpec,
        _ phase: KeyboardKeyEvent.Phase
    ) -> KeyboardKeyEvent {
        KeyboardKeyEvent(key: key, phase: phase)
    }
}
#endif
