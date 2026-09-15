#if canImport(UIKit)
import KeyboardLayout
import UIKit

@MainActor
public enum KeyboardMetrics {
    public static let phonePortraitBaseHeight: CGFloat = 294
    public static let phoneLandscapeBaseHeight: CGFloat = 226
    public static let padBaseHeight: CGFloat = 314

    public static func recommendedHeight(
        for traits: UITraitCollection,
        scale: CGFloat = 1
    ) -> CGFloat {
        recommendedHeight(for: .funputQWERTY, traits: traits, sizing: KeyboardSizingProfile(heightScale: scale))
    }

    public static func recommendedHeight(
        for layout: KeyboardLayout,
        traits: UITraitCollection,
        scale: CGFloat
    ) -> CGFloat {
        recommendedHeight(for: layout, traits: traits, sizing: KeyboardSizingProfile(heightScale: scale))
    }

    public static func recommendedHeight(
        for layout: KeyboardLayout,
        traits: UITraitCollection,
        sizing: KeyboardSizingProfile = .default,
        screenWidth: CGFloat? = nil
    ) -> CGFloat {
        let effective = effectiveSizing(sizing, traits: traits)
        if effective.keySizing == .system {
            return systemHeight(for: layout, sizing: effective, screenWidth: screenWidth ?? portraitScreenWidth)
        }
        let baseHeight: CGFloat
        if traits.userInterfaceIdiom == .pad {
            baseHeight = padBaseHeight
        } else if traits.verticalSizeClass == .compact {
            baseHeight = phoneLandscapeBaseHeight
        } else {
            baseHeight = phonePortraitBaseHeight
        }
        return height(for: layout, baseHeight: baseHeight, scale: effective.heightScale)
    }

    public static func phonePortraitHeight(
        for layout: KeyboardLayout,
        sizing: KeyboardSizingProfile = .default,
        screenWidth: CGFloat? = nil
    ) -> CGFloat {
        if sizing.keySizing == .system {
            return systemHeight(for: layout, sizing: sizing, screenWidth: screenWidth ?? portraitScreenWidth)
        }
        return height(for: layout, baseHeight: phonePortraitBaseHeight, scale: sizing.heightScale)
    }

    /// The sizing a surface actually lays out with. Apple's metrics were only measured
    /// for iPhone in portrait, so landscape and iPad keep Funput's own.
    public static func effectiveSizing(
        _ sizing: KeyboardSizingProfile,
        traits: UITraitCollection
    ) -> KeyboardSizingProfile {
        guard sizing.keySizing == .system else { return sizing }
        let isPhonePortrait = traits.userInterfaceIdiom != .pad && traits.verticalSizeClass != .compact
        return isPhonePortrait ? sizing : .default
    }

    /// The short side of the screen, which picks the row heights in system sizing.
    static var portraitScreenWidth: CGFloat {
        let bounds = UIScreen.main.bounds
        return min(bounds.width, bounds.height)
    }

    /// Rows at Apple's measured heights, so the keyboard grows or shrinks with its rows
    /// instead of squeezing them into Funput's budget.
    private static func systemHeight(
        for layout: KeyboardLayout,
        sizing: KeyboardSizingProfile,
        screenWidth: CGFloat
    ) -> CGFloat {
        let rows = SystemKeyMetrics.rowsHeight(screenWidth: screenWidth, rowCount: layout.rows.count)
        return sizing.verticalPadding * 2
            + rows
            + sizing.verticalGap * CGFloat(max(layout.rows.count - 1, 0))
            + (layout.toolbar == nil ? 0 : sizing.toolbarChrome)
    }

    private static func height(
        for layout: KeyboardLayout,
        baseHeight: CGFloat,
        scale: CGFloat
    ) -> CGFloat {
        let verticalPadding: CGFloat = 12
        // Read from the profile the geometry lays out with, so the strip reserved here
        // and the band actually drawn can never disagree.
        let toolbarChrome = KeyboardSizingProfile.default.toolbarChrome
        let rowGap: CGFloat = 7
        let standardRows: CGFloat = 5
        let standardRowHeight = (
            baseHeight - verticalPadding - toolbarChrome - rowGap * (standardRows - 1)
        ) / standardRows
        let rowCount = CGFloat(layout.rows.count)
        let targetHeight = verticalPadding
            + standardRowHeight * rowCount
            + rowGap * CGFloat(max(layout.rows.count - 1, 0))
            + (layout.toolbar == nil ? 0 : toolbarChrome)
        return targetHeight * min(max(scale, 0.85), 1.2)
    }
}
#endif
