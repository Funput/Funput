#if canImport(UIKit)
import FunputShared
import Testing
import UIKit
@testable import KeyboardRenderer

/// Holds the emoji panel to the memory it was measured at.
///
/// CoreText never gives back an emoji bitmap once decoded, so what each glyph costs
/// is what browsing the catalog costs for the rest of the process. At the 64 px
/// strike a glyph measured about 16 KB; at the 96 px strike a 3x screen picks by
/// default, about 48 KB. The budget sits between them, so a change that puts the
/// panel back on the expensive strike fails here instead of on someone's phone.
@MainActor
@Suite("Emoji memory budget")
struct EmojiMemoryBudgetTests {
    /// Generous against the measured 16 KB, well short of the 48 KB regression.
    static let budgetPerGlyph: Double = 28 * 1024

    @Test("Drawing the catalog stays on the cheap strike")
    func glyphCost() throws {
        // The end of the catalog, which other tests are least likely to have drawn.
        let glyphs = EmojiCatalog.bundled.emojis.suffix(301).map(\.glyph)
        // One glyph first, so the font and drawing setup are not counted as cache.
        draw(glyphs[0])
        let before = try #require(MemoryFootprint.current())
        glyphs.dropFirst().forEach(draw)
        let after = try #require(MemoryFootprint.current())
        let perGlyph = Double(Int64(after.physical) - Int64(before.physical))
            / Double(glyphs.count - 1)
        #expect(perGlyph < Self.budgetPerGlyph, "\(Int(perGlyph / 1024)) KB per glyph")
    }

    /// Draws `glyph` the way a cell does, then lets the layer go. The flush is what
    /// returns its backing store; without it the layers' own bitmaps would be counted
    /// as cache.
    private func draw(_ glyph: String) {
        autoreleasepool {
            let layer = EmojiGlyphLayer()
            layer.bounds = CGRect(x: 0, y: 0, width: 44, height: 44)
            layer.glyph = glyph
            layer.display()
        }
        CATransaction.flush()
    }

    @Test("A glyph layer draws at the emoji scale, not the screen's")
    func layerScale() {
        let cell = EmojiCell(frame: CGRect(x: 0, y: 0, width: 44, height: 44))
        let window = UIWindow(frame: CGRect(x: 0, y: 0, width: 100, height: 100))
        window.addSubview(cell)
        cell.apply(EmojiItem(glyph: "😀", name: "grinning face", category: .smileysPeople))
        cell.layoutIfNeeded()
        cell.glyphLayer.display()
        #expect(cell.glyphLayer.contentsScale == EmojiGlyphRaster.scale)
        #expect(cell.glyphLayer.contents != nil)
        window.isHidden = true
    }

    @Test("A browsing panel drops its cells' prefetch and its search state")
    func browsingHoldsNoSearch() {
        let view = EmojiKeyboardView(catalog: EmojiCatalog(version: "test", emojis: []))
        #expect(!view.collectionView.isPrefetchingEnabled)
        #expect(view.searchIndex == nil)
        #expect(view.searchKeyboard == nil)
    }
}
#endif
