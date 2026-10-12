#if canImport(UIKit)
import ThemeSchema
import UIKit

/// The fonts a theme asks key labels to be drawn in.
///
/// Every kind of label keeps the weight it has always had — letters regular, modifier text
/// semibold, Space medium — and the theme's ``ThemeLabelWeight`` moves all of them along the
/// system weight scale together. At ``ThemeLabelWeight/regular`` nothing moves, so a theme that
/// never set a weight draws exactly what it drew before. This keeps the iOS hierarchy (modifier
/// text two steps above letters) rather than Android's one-step rule; the four choices and what
/// they mean to the user are the same on both.
enum KeyLabelTypography {
    private static let scale: [UIFont.Weight] = [
        .ultraLight, .thin, .light, .regular, .medium, .semibold, .bold, .heavy, .black,
    ]
    /// Heaviest weight a shift may reach; black glyphs fill in on a small key.
    private static let heaviest = 7

    /// A system font at `size` points scaled by the theme, at `base` shifted by its weight.
    static func font(size: CGFloat, base: UIFont.Weight, theme: ResolvedTheme) -> UIFont {
        .systemFont(ofSize: size * theme.fontScale, weight: weight(base, shiftedBy: theme.labelWeight))
    }

    /// `font` made no taller than `height`, but never smaller than its size at a scale of 1.
    ///
    /// Labels shrink to fit their width but not their height, so a theme's larger text could
    /// spill past a short key (landscape, a reduced keycap). A theme may enlarge text only as far
    /// as the key has room; at a scale of 1 nothing changes, whatever the key's height.
    static func fitting(_ font: UIFont, scale: Double, height: CGFloat) -> UIFont {
        guard scale > 1, font.lineHeight > height, height > 0 else { return font }
        let unscaled = font.pointSize / scale
        return font.withSize(max(unscaled, font.pointSize * height / font.lineHeight))
    }

    static func weight(_ base: UIFont.Weight, shiftedBy labelWeight: ThemeLabelWeight) -> UIFont.Weight {
        guard let index = scale.firstIndex(of: base) else { return base }
        return scale[min(max(index + labelWeight.steps, 0), heaviest)]
    }
}

private extension ThemeLabelWeight {
    /// Steps along the system scale; bold is three above regular, as `UIFont.Weight.bold` is.
    var steps: Int {
        switch self {
        case .light: -1
        case .regular: 0
        case .medium: 1
        case .bold: 3
        }
    }
}
#endif
