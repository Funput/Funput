#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@MainActor
struct RendererSemanticsTests {
    private let traits = UITraitCollection(userInterfaceStyle: .dark)

    @Test("Placeholder keeps geometry but has no interaction or accessibility")
    func placeholder() {
        let layout = PasswordKeyboardLayouts.pin(.telex)
        let key = layout.rows.flatMap(\.keys).first { $0.role == .placeholder }!
        let control = KeyboardKeyControl(spec: key)
        #expect(control.isHidden)
        #expect(!control.isUserInteractionEnabled)
        #expect(!control.isAccessibilityElement)
    }

    @Test("All VNI modifiers render their secondary hints")
    func vniHint() {
        let keys = StandardKeyboardLayouts.letters(.vni).rows[0].keys
        for key in keys {
            let hint = key.secondaryLabel!
            #expect(labels(in: renderedControl(key: key)).contains(hint))
        }
    }

    @Test("Language-aware Space shows swipe affordances")
    func languageSpace() {
        let layout = StandardKeyboardLayouts.letters(.telex)
        let key = layout.rows[4].keys.first { $0.role == .space }!
        var presentation = KeyboardPresentation(layout: layout, language: .english)
        presentation.theme = .funputGlass
        let control = renderedControl(key: key, presentation: presentation)
        #expect(labels(in: control).contains("Tiếng Anh"))
        #expect(visibleImages(in: control).count >= 2)
    }

    @Test("Symbol Space uses the same language affordance")
    func symbolLanguageSpace() {
        let layout = SymbolKeyboardLayouts.primary(.vni)
        let key = layout.rows[4].keys.first { $0.role == .space }!
        let presentation = KeyboardPresentation(layout: layout, language: .english)
        let control = renderedControl(key: key, presentation: presentation)
        #expect(labels(in: control).contains("Tiếng Anh"))
        #expect(visibleImages(in: control).count >= 2)
    }

    @Test("Search Space supports Vietnamese and language switching")
    func searchLanguageSpace() {
        let layout = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            editorMode: .search
        )
        let key = layout.rows[4].keys.first { $0.role == .space }!
        let control = renderedControl(key: key, presentation: KeyboardPresentation(layout: layout))
        #expect(labels(in: control).contains("Tiếng Việt"))
        #expect(visibleImages(in: control).count >= 2)
    }

    @Test("Secure Space stays English without chevrons")
    func secureSpace() {
        let layout = PasswordKeyboardLayouts.text(.telex)
        let key = layout.rows[4].keys.first { $0.role == .space }!
        let control = renderedControl(key: key, presentation: KeyboardPresentation(layout: layout))
        #expect(labels(in: control).contains("English"))
        #expect(visibleImages(in: control).isEmpty)
    }

    @Test("Standard Enter actions render the expected icons")
    func enterIcons() {
        let layout = StandardKeyboardLayouts.letters(.telex)
        let key = layout.rows[4].keys.first { $0.role == .enter }!
        let cases: [(KeyboardEnterAction, String)] = [
            (.newLine, "return"),
            (.go, "arrow.right"),
            (.search, "magnifyingglass"),
            (.send, "paperplane"),
            (.next, "arrow.right.to.line"),
            (.done, "checkmark"),
            (.previous, "arrow.left.to.line"),
        ]
        for (action, symbolName) in cases {
            let presentation = KeyboardPresentation(layout: layout, enterAction: action)
            let control = renderedControl(key: key, presentation: presentation)
            let expected = UIImage(systemName: symbolName)
            #expect(visibleImages(in: control).contains { $0.image?.isEqual(expected) == true })
        }
    }

    @Test("Custom Enter action renders its label")
    func customEnter() {
        let layout = StandardKeyboardLayouts.letters(.telex)
        let key = layout.rows[4].keys.first { $0.role == .enter }!
        let presentation = KeyboardPresentation(layout: layout, enterAction: .custom("Apply"))
        #expect(labels(in: renderedControl(key: key, presentation: presentation)).contains("Apply"))
    }

    private func renderedControl(
        key: KeySpec,
        presentation: KeyboardPresentation = KeyboardPresentation()
    ) -> KeyboardKeyControl {
        let control = KeyboardKeyControl(spec: key)
        control.apply(presentation: presentation, traits: traits)
        return control
    }

    private func labels(in view: UIView) -> [String] {
        view.subviews.flatMap { child -> [String] in
            let own = (child as? UILabel)?.text.map { [$0] } ?? []
            return own + labels(in: child)
        }
    }

    private func visibleImages(in view: UIView) -> [UIImageView] {
        view.subviews.flatMap { child -> [UIImageView] in
            let own = (child as? UIImageView).map { $0.isHidden ? [] : [$0] } ?? []
            return own + visibleImages(in: child)
        }
    }
}
#endif
