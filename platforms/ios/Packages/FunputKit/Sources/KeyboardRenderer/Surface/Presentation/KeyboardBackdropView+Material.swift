#if canImport(UIKit)
import ThemeSchema
import UIKit

extension KeyboardBackdropView {
    /// Where the surface behind the keys gets its material from.
    enum MaterialSource: Equatable {
        /// The keyboard host's own backdrop shows through.
        case host
        /// Funput blurs what is behind it, following its own trait collection.
        case blur
        /// No material; the themed fill is the whole backdrop.
        case none
    }

    /// Transparent translucent themes borrow the keyboard host's backdrop, but only while
    /// the appearance follows the host. A pinned appearance must never show the host's
    /// colors, otherwise a light host would sit behind a dark-pinned keyboard.
    static func borrowsHostBackdrop(
        theme: ResolvedTheme, usesImage: Bool,
        traits: UITraitCollection, pinsAppearance: Bool
    ) -> Bool {
        !pinsAppearance && !usesImage && theme.material == .translucent
            && theme.backgroundStart.uiColor(for: traits).cgColor.alpha == 0
            && theme.backgroundEnd.uiColor(for: traits).cgColor.alpha == 0
    }

    /// Other themes borrow the host only for unpinned Liquid Glass; pinned themes bring
    /// their own material.
    static func materialSource(
        theme: ResolvedTheme, usesImage: Bool,
        usesHostBackdrop: Bool, pinsAppearance: Bool
    ) -> MaterialSource {
        if usesHostBackdrop { return .host }
        let reducesTransparency = UIAccessibility.isReduceTransparencyEnabled
        if #available(iOS 26.0, *), theme.material == .glass, !reducesTransparency, !pinsAppearance {
            return .host
        }
        let solid = theme.material == .solid || reducesTransparency || usesImage
        return solid ? .none : .blur
    }
}
#endif
