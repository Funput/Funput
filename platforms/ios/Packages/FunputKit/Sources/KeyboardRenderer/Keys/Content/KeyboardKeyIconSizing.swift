import CoreGraphics
import KeyboardLayout

/// How big the symbol of an icon-only key is drawn.
///
/// The stock keyboard prints the globe, emoji and hide-keyboard symbols at a fixed, small
/// size however tall the key is. Filling the key minus a proportional inset instead makes
/// them balloon on the iPad's tall keys.
enum KeyboardKeyIconSizing {
    /// Side of the square the symbol is fitted into, measured against the stock iPad keyboard.
    static let symbolSide: CGFloat = 22

    static func isFixedSize(_ role: KeyRole) -> Bool {
        switch role {
        case .globe, .emoji, .dismissKeyboard: true
        default: false
        }
    }

    /// The icon frame for a key: a fixed centered square for the keys above, `fallback`
    /// (the proportional frame) for every other key.
    static func frame(for role: KeyRole, in bounds: CGRect, fallback: CGRect) -> CGRect {
        guard isFixedSize(role) else { return fallback }
        let side = min(symbolSide, bounds.width, bounds.height)
        return CGRect(
            x: bounds.midX - side / 2,
            y: bounds.midY - side / 2,
            width: side,
            height: side
        )
    }
}
