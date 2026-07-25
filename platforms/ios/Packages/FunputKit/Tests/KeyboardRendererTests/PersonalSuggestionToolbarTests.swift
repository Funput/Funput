#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@MainActor
@Suite("Personal suggestion toolbar")
struct PersonalSuggestionToolbarTests {
    @Test("Keeps logo utilities and emits one candidate")
    func contentAndSelection() {
        let toolbar = KeyboardToolbarView(frame: CGRect(x: 0, y: 0, width: 360, height: 44))
        toolbar.apply(spec: .withSystemInputMode, theme: .funputGlass, traits: .init())
        let values = candidates(3)
        toolbar.updateSuggestions(values)
        toolbar.layoutIfNeeded()
        var selected: [KeyboardSuggestionCandidate] = []
        toolbar.onSuggestionSelected = { selected.append($0) }

        let buttons = visibleButtons(in: toolbar)
        #expect(buttons.contains { $0.accessibilityLabel == "Chuyển bàn phím" })
        #expect(buttons.contains { $0.accessibilityLabel == "Biểu tượng cảm xúc" })
        let suggestion = buttons.first { $0.accessibilityLabel == "Gợi ý, từ0" }
        #expect(suggestion != nil)
        suggestion.map(tap)
        #expect(selected == [values[0]])
    }

    @Test("Suggestion updates do not rebuild key controls")
    func hotUpdate() {
        let layout = StandardKeyboardLayouts.letters(.telex)
        let surface = KeyboardSurfaceView(presentation: KeyboardPresentation(layout: layout))
        let before = surface.keyControls.mapValues { ObjectIdentifier($0) }
        surface.updateSuggestions(candidates(3))
        #expect(surface.keyControls.mapValues { ObjectIdentifier($0) } == before)
    }

    @Test("Narrow toolbar keeps highest-ranked candidates that fit")
    func adaptiveWidth() {
        let toolbar = KeyboardToolbarView(frame: CGRect(x: 0, y: 0, width: 220, height: 44))
        toolbar.apply(spec: .withSystemInputMode, theme: .funputGlass, traits: .init())
        toolbar.updateSuggestions(candidates(3))
        toolbar.layoutIfNeeded()
        let labels = visibleButtons(in: toolbar).compactMap(\.accessibilityLabel)
        #expect(labels.contains("Gợi ý, từ0"))
        #expect(!labels.contains("Gợi ý, từ2"))
    }

    private func candidates(_ count: Int) -> [KeyboardSuggestionCandidate] {
        (0..<count).map { KeyboardSuggestionCandidate(text: "từ\($0)", generation: 7) }
    }

    /// Fire a control's registered `touchUpInside` actions.
    ///
    /// `UIControl.sendActions(for:)` dispatches through `UIApplication.shared`, which a
    /// SwiftPM test bundle has no host app for — the actions are silently dropped. So
    /// invoke the target/action pairs the control actually holds.
    private func tap(_ control: UIControl) {
        for target in control.allTargets {
            let actions = control.actions(forTarget: target, forControlEvent: .touchUpInside)
            for action in actions ?? [] {
                _ = (target as AnyObject).perform(Selector(action), with: control)
            }
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
