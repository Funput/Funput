#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import UIKit

/// The iPad prints its globe, emoji and hide-keyboard symbols larger than the phone, tucked
/// into a bottom corner on the line its corner words sit on.
@MainActor
struct PadCornerIconTests {
    @Test("On iPad the globe and emoji grow into the bottom-left corner, hide into the bottom-right")
    func cornerIcons() {
        let bounds = CGRect(x: 0, y: 0, width: 110, height: 80)
        let fallback = bounds.insetBy(dx: 16, dy: 16)
        let inset = KeyboardKeyIconSizing.horizontalInset(in: bounds)
        let line = bounds.maxY - KeyboardKeyIconSizing.bottomInset(in: bounds)
        for role in [KeyRole.globe, .emoji] {
            let frame = KeyboardKeyIconSizing.frame(
                for: role, placement: .bottomLeading, in: bounds, imageSize: CGSize(width: 20, height: 20),
                fallback: fallback
            )
            #expect(frame.width == KeyboardKeyIconSizing.cornerGlobeSide)
            #expect(frame.width > KeyboardKeyIconSizing.symbolSide)
            #expect(frame.minX == inset && frame.maxY == line)
        }
        let hide = KeyboardKeyIconSizing.frame(
            for: .dismissKeyboard, placement: .bottomTrailing, in: bounds, imageSize: CGSize(width: 30, height: 24),
            fallback: fallback
        )
        #expect(abs(hide.width - KeyboardKeyIconSizing.cornerDismissSide) < 0.001 && hide.height < hide.width)
        #expect(abs(hide.maxX - (bounds.maxX - inset)) < 0.001 && abs(hide.maxY - line) < 0.001)
    }

    @Test("A corner symbol key keeps its symbol instead of turning into a word")
    func cornerSymbolContent() throws {
        let view = KeyboardKeyContentView(frame: CGRect(x: 0, y: 0, width: 110, height: 80))
        let spec = KeySpec(id: "globe", label: "", role: .globe, labelPlacement: .bottomLeading)
        view.apply(spec: spec, presentation: KeyboardPresentation(), traits: UITraitCollection())
        view.layoutIfNeeded()
        let icon = try #require(view.subviews.compactMap { $0 as? UIImageView }.first)
        #expect(!icon.isHidden && icon.image != nil)
        #expect(icon.frame.minX < view.bounds.midX && icon.frame.midY > view.bounds.midY)
    }
}
#endif
