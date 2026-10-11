#if canImport(UIKit)
import KeyboardLayout
import UIKit

extension KeyboardMetrics {
    /// The ratio of a key's height to its width on the stock iPad keyboard, measured on a
    /// 1024pt-wide landscape screen: 64pt tall keys of about 70pt width.
    private static let padKeyAspect: CGFloat = 0.9
    /// Row heights stop growing here, so a large iPad Pro does not get a keyboard that
    /// covers half the screen, and stop shrinking here, so a narrow split-screen window
    /// keeps keys that can be tapped.
    private static let padRowHeightRange: ClosedRange<CGFloat> = 50...68
    /// Columns the widest letter row spans: ten letters plus a key and a half at each end.
    private static let padLetterColumns: CGFloat = 13

    /// An iPad keyboard sized by its rows rather than by a fixed total.
    ///
    /// The phone budget (a fixed total shared out among the rows) leaves iPad keys short
    /// and wide, however wide the screen. Here the key height follows the key width the
    /// way the stock keyboard's does, then the height setting scales the whole keyboard.
    static func padHeight(
        for layout: KeyboardLayout,
        scale: CGFloat,
        screenWidth: CGFloat
    ) -> CGFloat {
        let profile = KeyboardSizingProfile.default
        let rowCount = CGFloat(layout.rows.count)
        let target = profile.topPadding(for: layout)
            + (layout.toolbar == nil ? 0 : profile.toolbarChrome)
            + padRowHeight(screenWidth: screenWidth, profile: profile) * rowCount
            + profile.verticalGap * max(rowCount - 1, 0)
        return target * min(max(scale, 0.85), 1.2)
    }

    private static func padRowHeight(
        screenWidth: CGFloat,
        profile: KeyboardSizingProfile
    ) -> CGFloat {
        let gaps = profile.horizontalGap * (padLetterColumns - 1)
        let keyWidth = (screenWidth - profile.horizontalPadding * 2 - gaps) / padLetterColumns
        let height = keyWidth * padKeyAspect
        return min(max(height, padRowHeightRange.lowerBound), padRowHeightRange.upperBound)
    }

    /// The width the keyboard is laid out across, which in the extension is the screen's.
    static var currentScreenWidth: CGFloat { UIScreen.main.bounds.width }
}
#endif
