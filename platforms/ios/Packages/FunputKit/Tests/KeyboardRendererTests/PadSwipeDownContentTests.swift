#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

@MainActor
struct PadSwipeDownContentTests {
    private func view() -> KeyboardKeyContentView {
        let view = KeyboardKeyContentView(frame: CGRect(x: 0, y: 0, width: 78, height: 64))
        let spec = KeySpec(id: "q", label: "q", role: .character, secondaryLabel: "1", labelPlacement: .bottomCenter)
        view.apply(spec: spec, presentation: KeyboardPresentation(), traits: UITraitCollection())
        view.layoutIfNeeded()
        return view
    }

    @Test("A full pull hides the letter and puts the hint in its place at its size")
    func fullPull() {
        let view = view()
        view.setSwipeProgress(1, animated: false)
        #expect(view.label.alpha == 0 && view.secondaryLabel.alpha == 0)
        #expect(view.swipeHintLabel.alpha == 1 && view.swipeHintLabel.transform == .identity)
        #expect(view.swipeHintLabel.text == "1" && view.swipeHintLabel.center == view.label.center)
    }

    @Test("Mid-pull the hint is on its way, and releasing puts the key back at rest")
    func midPullAndRest() {
        let view = view()
        view.setSwipeProgress(0.5, animated: false)
        #expect(view.swipeHintLabel.transform != .identity)
        #expect(view.label.alpha == 0.5)
        view.layoutSubviews()
        #expect(view.label.alpha == 0.5)
        view.setSwipeProgress(0, animated: false)
        #expect(view.label.transform == .identity && view.label.alpha == 1)
        #expect(view.secondaryLabel.transform == .identity && view.swipeHintLabel.alpha == 0)
    }
}
#endif
