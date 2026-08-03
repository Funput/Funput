#if canImport(UIKit)
@testable import KeyboardRenderer
import KeyboardLayout
import Testing
import ThemeSchema
import UIKit

@MainActor
@Suite("Clipboard chip toolbar")
struct ClipboardChipToolbarTests {
    @Test("Chip takes the shared region when nothing is being typed")
    func showsWithoutSuggestions() throws {
        let toolbar = makeToolbar()
        toolbar.updateClipboardHint(.text)
        toolbar.layoutIfNeeded()
        #expect(try #require(chip(in: toolbar)).isHidden == false)
    }

    @Test("Suggestions win the shared region, and give it back when they clear")
    func suggestionsWin() throws {
        let toolbar = makeToolbar()
        toolbar.updateClipboardHint(.link)
        toolbar.updateSuggestions([KeyboardSuggestionCandidate(text: "chào", generation: 1)])
        #expect(try #require(chip(in: toolbar)).isHidden == true)

        toolbar.updateSuggestions([])
        #expect(try #require(chip(in: toolbar)).isHidden == false)
    }

    @Test("No hint means no chip, whatever the suggestions are doing")
    func hiddenWithoutHint() throws {
        let toolbar = makeToolbar()
        toolbar.updateClipboardHint(nil)
        #expect(try #require(chip(in: toolbar)).isHidden == true)

        toolbar.updateSuggestions([])
        #expect(try #require(chip(in: toolbar)).isHidden == true)
    }

    /// 44pt times the smallest allowed height scale (0.85) is the tightest the
    /// toolbar ever gets, and the system paste control's intrinsic height is larger
    /// than that — so it has to be scaled rather than left to overflow.
    @Test("Everything stays inside the shortest toolbar")
    func fitsShortestToolbar() throws {
        let toolbar = makeToolbar(height: 44 * 0.85)
        toolbar.updateClipboardHint(.text)
        toolbar.layoutIfNeeded()

        let chipView = try #require(chip(in: toolbar))
        #expect(chipView.frame.maxY <= toolbar.bounds.maxY + 0.5)
        for control in chipView.subviews.compactMap({ $0 as? UIPasteControl }) {
            #expect(control.frame.height <= chipView.bounds.height + 0.5)
        }
    }

    /// The clipboard key sits where the globe used to. It steps aside for the same
    /// reason the chip does: while the user is typing, the toolbar belongs to
    /// suggestions.
    @Test("The clipboard key yields its slot while suggestions are showing")
    func clipboardKeyYieldsToSuggestions() throws {
        let toolbar = makeToolbar()
        toolbar.layoutIfNeeded()
        let key = try #require(
            buttons(in: toolbar).first { $0.accessibilityLabel == "Lịch sử clipboard" }
        )
        #expect(!key.isHidden)

        toolbar.updateSuggestions([KeyboardSuggestionCandidate(text: "chào", generation: 1)])
        #expect(key.isHidden)

        toolbar.updateSuggestions([])
        #expect(!key.isHidden)
    }

    /// SF Symbols size by cap height, so the tall clipboard glyph came out visibly
    /// bigger than the round emoji one until it was scaled to match.
    @Test("The clipboard key is no taller than the emoji key beside it")
    func toolbarSymbolsShareOneHeight() throws {
        let toolbar = makeToolbar()
        let all = buttons(in: toolbar)
        let clipboard = try #require(
            all.first { $0.accessibilityLabel == "Lịch sử clipboard" }?.currentImage
        )
        let emoji = try #require(
            all.first { $0.accessibilityLabel == "Biểu tượng cảm xúc" }?.currentImage
        )
        #expect(clipboard.size.height <= emoji.size.height + 0.5)
    }

    /// Turning the feature off has to take the toolbar key with it, not just the chip.
    @Test("Declining the feature hides the clipboard key even with no suggestions")
    func clipboardKeyHiddenWhenDisabled() throws {
        let toolbar = makeToolbar()
        toolbar.updateClipboardKeyVisible(false)
        toolbar.layoutIfNeeded()
        let key = try #require(
            buttons(in: toolbar).first { $0.accessibilityLabel == "Lịch sử clipboard" }
        )
        #expect(key.isHidden)

        toolbar.updateClipboardKeyVisible(true)
        #expect(!key.isHidden)
    }

    private func buttons(in view: UIView) -> [UIButton] {
        view.subviews.flatMap { child in
            (child as? UIButton).map { [$0] } ?? buttons(in: child)
        }
    }

    private func makeToolbar(height: CGFloat = 44) -> KeyboardToolbarView {
        let toolbar = KeyboardToolbarView(
            frame: CGRect(x: 0, y: 0, width: 393, height: height)
        )
        toolbar.apply(spec: .standard, theme: .funputGlass, traits: .init())
        return toolbar
    }

    private func chip(in toolbar: KeyboardToolbarView) -> KeyboardClipboardChipView? {
        toolbar.subviews.compactMap { $0 as? KeyboardClipboardChipView }.first
    }
}
#endif
