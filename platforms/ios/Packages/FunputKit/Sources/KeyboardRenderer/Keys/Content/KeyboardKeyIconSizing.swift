import CoreGraphics
import KeyboardLayout

/// How big the symbol of an icon-only key is drawn, and where.
///
/// The stock keyboard prints the globe, emoji and hide-keyboard symbols at a fixed size
/// however tall the key is. Filling the key minus a proportional inset instead makes them
/// balloon on the iPad's tall keys. The phone centers them; the iPad prints them larger and
/// tucks them into a bottom corner, on the line its corner words sit on.
enum KeyboardKeyIconSizing {
    /// Side of the square a centered symbol is fitted into.
    static let symbolSide: CGFloat = 22

    /// Sides of the boxes a corner symbol is fitted into, measured against the stock iPad
    /// keyboard: the hide-keyboard symbol is wider than the globe, so it gets a wider box.
    static let cornerGlobeSide: CGFloat = 26
    static let cornerDismissSide: CGFloat = 31

    static func isFixedSize(_ role: KeyRole) -> Bool {
        switch role {
        case .globe, .emoji, .dismissKeyboard: true
        default: false
        }
    }

    /// The icon frame for a key. The keys above get a fixed symbol, centered or anchored in
    /// the corner their placement names; every other key gets `fallback`, the proportional
    /// frame. `imageSize` keeps a wide symbol's bottom on the corner line rather than
    /// floating in the middle of a square box.
    static func frame(
        for role: KeyRole,
        placement: KeyLabelPlacement = .center,
        in bounds: CGRect,
        imageSize: CGSize? = nil,
        fallback: CGRect
    ) -> CGRect {
        guard isFixedSize(role) else { return fallback }
        guard placement.isCorner else { return centered(in: bounds) }
        let size = fitted(imageSize, side: role == .dismissKeyboard ? cornerDismissSide : cornerGlobeSide, in: bounds)
        let inset = horizontalInset(in: bounds)
        let x = placement == .bottomLeading ? bounds.minX + inset : bounds.maxX - inset - size.width
        let bottom = bounds.maxY - bottomInset(in: bounds)
        return CGRect(x: x, y: max(bounds.minY, bottom - size.height), width: size.width, height: size.height)
    }

    /// The same side margin as the corner words, so the globe lines up with `shift` above it.
    static func horizontalInset(in bounds: CGRect) -> CGFloat {
        min(12, bounds.width * 0.12)
    }

    /// The stock symbols rest about ten points above the key's bottom edge.
    static func bottomInset(in bounds: CGRect) -> CGFloat {
        min(10, bounds.height * 0.15)
    }

    private static func centered(in bounds: CGRect) -> CGRect {
        let side = min(symbolSide, bounds.width, bounds.height)
        return CGRect(x: bounds.midX - side / 2, y: bounds.midY - side / 2, width: side, height: side)
    }

    /// The image's aspect fitted into a `side` square, held inside the key.
    private static func fitted(_ imageSize: CGSize?, side: CGFloat, in bounds: CGRect) -> CGSize {
        let box = min(side, bounds.width - horizontalInset(in: bounds) * 2, bounds.height)
        guard box > 0 else { return .zero }
        guard let imageSize, imageSize.width > 0, imageSize.height > 0 else {
            return CGSize(width: box, height: box)
        }
        let scale = box / max(imageSize.width, imageSize.height)
        return CGSize(width: imageSize.width * scale, height: imageSize.height * scale)
    }
}
