#if canImport(UIKit)
@testable import KeyboardRenderer
import Testing
import ThemeSchema
import UIKit

/// The paste control is system-drawn, so a keyboard with nothing to paste must
/// reach its first frame without ever building one.
@MainActor
@Suite("Clipboard chip paste control")
struct ClipboardChipLazyControlTests {
    @Test("A new chip holds no paste control, themed or not")
    func noControlBeforeAnOffer() {
        let chip = KeyboardClipboardChipView()
        #expect(pasteControls(in: chip).isEmpty)

        chip.apply(theme: .funputGlass, traits: .init())
        chip.update(hint: nil)
        #expect(pasteControls(in: chip).isEmpty)
    }

    @Test("The first offer builds exactly one control")
    func firstOfferBuildsTheControl() {
        let chip = KeyboardClipboardChipView()
        chip.apply(theme: .funputGlass, traits: .init())
        chip.update(hint: .text)
        chip.update(hint: .link)
        #expect(pasteControls(in: chip).count == 1)
    }

    @Test("A theme change replaces the control rather than adding one")
    func themeChangeReplacesTheControl() throws {
        let chip = KeyboardClipboardChipView()
        chip.apply(theme: .funputGlass, traits: .init())
        chip.update(hint: .text)
        let first = try #require(pasteControls(in: chip).first)

        var recoloured = ResolvedTheme.funputGlass
        recoloured.accent = recoloured.secondaryLabel
        chip.apply(theme: recoloured, traits: .init())
        let second = try #require(pasteControls(in: chip).first)
        #expect(pasteControls(in: chip).count == 1)
        #expect(first !== second)
    }

    @Test("A withdrawn offer keeps the control for the next one")
    func withdrawnOfferKeepsTheControl() {
        let chip = KeyboardClipboardChipView()
        chip.update(hint: .text)
        chip.update(hint: nil)
        #expect(chip.isHidden)
        #expect(pasteControls(in: chip).count == 1)
    }

    private func pasteControls(in chip: KeyboardClipboardChipView) -> [UIPasteControl] {
        chip.subviews.compactMap { $0 as? UIPasteControl }
    }
}
#endif
