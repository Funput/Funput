#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@MainActor
struct RendererContentTests {
    private let traits = UITraitCollection(userInterfaceStyle: .dark)

    @Test("Special layout keys render their model labels")
    func specialKeyLabels() {
        let primary = SymbolKeyboardLayouts.primary(.telex)
        let secondary = SymbolKeyboardLayouts.secondary(.telex)
        let email = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            editorMode: .email
        )
        let cases: [(KeySpec, String)] = [
            (primary.rows[3].keys[0], "=\\<"),
            (secondary.rows[3].keys[0], "?123"),
            (primary.rows[4].keys[0], "ABC"),
            (email.rows[4].keys[4], ".com"),
        ]

        for (key, expected) in cases {
            #expect(visibleLabels(in: renderedControl(key)).contains(expected))
        }
    }

    @Test("Keypad characters and punctuation render as text")
    func keypadContent() {
        let phone = PhoneKeyboardLayouts.resolve(.telex)
        let keys = [
            phone.rows[0].keys[0],
            phone.rows[2].keys[3],
            phone.rows[3].keys[0],
            phone.rows[3].keys[2],
        ]
        #expect(keys.map(\.label) == ["1", "+", "*", "#"])
        for key in keys {
            #expect(visibleLabels(in: renderedControl(key)).contains(key.label))
        }
    }

    @Test("Toolbar renders the brand logo and switcher buttons")
    func toolbarContent() {
        let toolbar = KeyboardToolbarView()
        toolbar.apply(spec: .withSystemInputMode, theme: .funputGlass, traits: traits)

        // The logo is a decorative image view, so only the switcher buttons show.
        #expect(!toolbar.isHidden)
        #expect(visibleButtons(in: toolbar).compactMap(\.accessibilityLabel) == [
            "Chuyển bàn phím", "Cài đặt", "Biểu tượng cảm xúc",
        ])

        toolbar.apply(spec: .standard, theme: .funputGlass, traits: traits)
        #expect(visibleButtons(in: toolbar).compactMap(\.accessibilityLabel) == [
            "Cài đặt", "Biểu tượng cảm xúc",
        ])

        toolbar.apply(spec: nil, theme: .funputGlass, traits: traits)
        #expect(toolbar.isHidden)
    }

    @Test("Toolbar emits press and release phases")
    func toolbarInteraction() {
        let toolbar = KeyboardToolbarView()
        var events: [KeyboardKeyEvent] = []
        toolbar.onEvent = { events.append($0) }
        toolbar.apply(spec: .standard, theme: .funputGlass, traits: traits)

        let settingsButton = visibleButtons(in: toolbar)[0]
        settingsButton.sendActions(for: .touchDown)
        settingsButton.sendActions(for: .touchUpInside)

        #expect(events.map(\.phase) == [.pressed, .released])
        #expect(events.map(\.key.role) == [.settings, .settings])
    }

    private func renderedControl(_ key: KeySpec) -> KeyboardKeyControl {
        let control = KeyboardKeyControl(spec: key)
        control.apply(presentation: KeyboardPresentation(), traits: traits)
        return control
    }

    private func visibleLabels(in view: UIView) -> [String] {
        view.subviews.flatMap { child -> [String] in
            let own = (child as? UILabel).flatMap {
                !$0.isHidden ? $0.text.map { [$0] } : nil
            } ?? []
            return own + visibleLabels(in: child)
        }
    }

    private func visibleButtons(in view: UIView) -> [UIButton] {
        view.subviews.flatMap { child -> [UIButton] in
            let own = (child as? UIButton).map { $0.isHidden ? [] : [$0] } ?? []
            return own + visibleButtons(in: child)
        }
    }
}
#endif
