#if canImport(UIKit)
import UIKit

/// How the emoji panels rasterize a glyph, chosen for memory before sharpness.
///
/// CoreText keeps every colour emoji bitmap it decodes for the life of the process,
/// keyed by glyph and by the font strike the drawing resolves to. Releasing the font,
/// the cell or the whole panel gives none of it back, so a keyboard that lets someone
/// browse the catalog ends up holding one bitmap per emoji ever shown.
///
/// The strike is therefore the only lever. Measured on iOS 27, a 3x screen drawing at
/// 32 pt resolves to the 96 px strike: about 48 KB a glyph, 90 MB for the catalog,
/// which alone is past what a keyboard extension may use. At 2x it resolves to the
/// 64 px strike: about 16 KB a glyph, 30 MB in all, and indistinguishable at cell
/// size. Every emoji the panels draw goes through here so they share that one strike.
enum EmojiGlyphRaster {
    /// Pixels per point of the emoji bitmaps, whatever the screen's own scale.
    static let scale: CGFloat = 2
    static let pointSize: CGFloat = 32
    /// How far a wide sequence (a flag, a family) may shrink to fit its cell.
    static let minimumFitScale: CGFloat = 0.7

    /// Draws `glyph` centred in `bounds` into a UIKit-oriented context, shrunk to the
    /// width when it is wider.
    static func draw(_ glyph: String, in bounds: CGRect, context: CGContext) {
        let text = NSAttributedString(
            string: glyph,
            attributes: [.font: UIFont.systemFont(ofSize: pointSize)]
        )
        let size = text.size()
        guard size.width > 0, size.height > 0, bounds.width > 0 else { return }
        let fit = min(1, max(minimumFitScale, bounds.width / size.width))
        UIGraphicsPushContext(context)
        defer { UIGraphicsPopContext() }
        context.saveGState()
        defer { context.restoreGState() }
        context.translateBy(x: bounds.midX, y: bounds.midY)
        context.scaleBy(x: fit, y: fit)
        text.draw(at: CGPoint(x: -size.width / 2, y: -size.height / 2))
    }
}
#endif
