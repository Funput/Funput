#if canImport(UIKit)
import UIKit

/// One emoji, drawn at ``EmojiGlyphRaster/scale``.
///
/// A sublayer and not a view on purpose: UIKit resets a view's layer to the screen's
/// scale every time the view moves to a window, which would put the glyph back on
/// the expensive strike. A plain sublayer keeps the scale it is given. The backing
/// store shrinks with it, from 3x to 2x of the cell.
final class EmojiGlyphLayer: CALayer {
    var glyph: String? {
        didSet {
            guard glyph != oldValue else { return }
            setNeedsDisplay()
        }
    }

    override init() {
        super.init()
        contentsScale = EmojiGlyphRaster.scale
        needsDisplayOnBoundsChange = true
        // A recycled cell must not fade from the emoji it showed before.
        actions = ["contents": NSNull(), "bounds": NSNull(), "position": NSNull()]
    }

    override init(layer: Any) {
        super.init(layer: layer)
        glyph = (layer as? EmojiGlyphLayer)?.glyph
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func draw(in context: CGContext) {
        guard let glyph else { return }
        EmojiGlyphRaster.draw(glyph, in: bounds, context: context)
    }
}
#endif
