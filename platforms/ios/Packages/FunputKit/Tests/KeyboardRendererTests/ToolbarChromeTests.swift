#if canImport(UIKit)
import KeyboardLayout
@testable import KeyboardRenderer
import Testing
import ThemeSchema
import UIKit

/// The toolbar band is described twice — once by the sizing profile the geometry lays
/// out with, once by the height budget that reserves room for it. They have to agree,
/// or the keyboard either clips its first key row or leaves a blank strip above it.
@MainActor
struct ToolbarChromeTests {
    @Test("The toolbar costs exactly the band the geometry draws plus its gap")
    func toolbarCostsItsChrome() {
        let shown = KeyboardLayoutResolver.resolve(inputMethod: .telex, mode: .letters)
        let hidden = KeyboardLayoutResolver.resolve(
            inputMethod: .telex,
            mode: .letters,
            showsToolbar: false
        )
        expectClose(
            KeyboardMetrics.phonePortraitHeight(for: shown)
                - KeyboardMetrics.phonePortraitHeight(for: hidden),
            KeyboardSizingProfile.default.toolbarChrome
        )
    }

    @Test("A tap in the padding above the band still reaches the toolbar")
    func paddingAboveTheBandIsTappable() {
        let toolbar = KeyboardToolbarView(
            frame: CGRect(x: 0, y: 0, width: 360, height: KeyboardSizingProfile.default.toolbarHeight)
        )
        toolbar.apply(spec: .standard, theme: .funputGlass, traits: .init())
        toolbar.layoutIfNeeded()
        let x = toolbar.emojiButton.frame.midX

        #expect(toolbar.hitTest(CGPoint(x: x, y: -4), with: nil) === toolbar.emojiButton)
        // Only the padding, though — the keycap touch surface owns everything further out.
        #expect(toolbar.hitTest(CGPoint(x: x, y: -40), with: nil) == nil)
    }

    private func expectClose(_ actual: CGFloat, _ expected: CGFloat) {
        #expect(abs(actual - expected) <= 0.01)
    }
}
#endif
