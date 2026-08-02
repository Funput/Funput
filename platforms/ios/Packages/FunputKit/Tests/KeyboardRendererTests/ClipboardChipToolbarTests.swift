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
