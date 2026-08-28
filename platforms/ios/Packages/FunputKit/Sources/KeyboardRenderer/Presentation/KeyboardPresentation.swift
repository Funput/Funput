#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

public struct KeyboardPresentation: Hashable, Sendable {
    public var layout: KeyboardLayout
    public var sizing: KeyboardSizingProfile
    public var theme: ResolvedTheme
    public var blendsSystemEdge: Bool
    public var shiftState: ShiftState
    public var language: KeyboardLanguage
    public var enterAction: KeyboardEnterAction
    public var isHapticFeedbackEnabled: Bool
    public var isKeySoundEnabled: Bool
    public var showsKeyPreviews: Bool
    /// Double-tap space, spacebar cursor panning and swipe-to-delete-word.
    public var areSmartGesturesEnabled: Bool

    public init(
        layout: KeyboardLayout = .funputQWERTY,
        sizing: KeyboardSizingProfile = .default,
        theme: ResolvedTheme = .funputGlass,
        blendsSystemEdge: Bool = false,
        shiftState: ShiftState = .lowercase,
        language: KeyboardLanguage = .vietnamese,
        enterAction: KeyboardEnterAction = .newLine,
        isHapticFeedbackEnabled: Bool = true,
        isKeySoundEnabled: Bool = false,
        showsKeyPreviews: Bool = true,
        areSmartGesturesEnabled: Bool = true
    ) {
        self.layout = layout
        self.sizing = sizing
        self.theme = theme
        self.blendsSystemEdge = blendsSystemEdge
        self.shiftState = shiftState
        self.language = language
        self.enterAction = enterAction
        self.isHapticFeedbackEnabled = isHapticFeedbackEnabled
        self.isKeySoundEnabled = isKeySoundEnabled
        self.showsKeyPreviews = showsKeyPreviews
        self.areSmartGesturesEnabled = areSmartGesturesEnabled
    }
}

public struct KeyboardKeyEvent: Sendable {
    public enum Phase: Equatable, Sendable {
        case pressed
        case repeated
        case swiped(KeySwipeAction)
        /// Caret movement from the spacebar trackpad, on both axes.
        case cursorMoved(CursorPanStep)
        /// One word rubbed away by a leftward drag on Backspace.
        case deletedWord
        case alternateSelected(KeyAlternate)
        case released
        case cancelled
    }

    public let key: KeySpec
    public let phase: Phase

    public init(key: KeySpec, phase: Phase) {
        self.key = key
        self.phase = phase
    }
}

@MainActor
public enum KeyboardMetrics {
    public static let phonePortraitBaseHeight: CGFloat = 304
    public static let phoneLandscapeBaseHeight: CGFloat = 236
    public static let padBaseHeight: CGFloat = 324

    public static func recommendedHeight(
        for traits: UITraitCollection,
        scale: CGFloat = 1
    ) -> CGFloat {
        recommendedHeight(for: .funputQWERTY, traits: traits, scale: scale)
    }

    public static func recommendedHeight(
        for layout: KeyboardLayout,
        traits: UITraitCollection,
        scale: CGFloat = 1
    ) -> CGFloat {
        let baseHeight: CGFloat
        if traits.userInterfaceIdiom == .pad {
            baseHeight = padBaseHeight
        } else if traits.verticalSizeClass == .compact {
            baseHeight = phoneLandscapeBaseHeight
        } else {
            baseHeight = phonePortraitBaseHeight
        }
        return height(for: layout, baseHeight: baseHeight, scale: scale)
    }

    public static func phonePortraitHeight(
        for layout: KeyboardLayout,
        scale: CGFloat = 1
    ) -> CGFloat {
        height(for: layout, baseHeight: phonePortraitBaseHeight, scale: scale)
    }

    private static func height(
        for layout: KeyboardLayout,
        baseHeight: CGFloat,
        scale: CGFloat
    ) -> CGFloat {
        let verticalPadding: CGFloat = 12
        let toolbarChrome: CGFloat = 50
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

extension AdaptiveThemeColor {
    func uiColor(for traits: UITraitCollection) -> UIColor {
        let rgba = traits.userInterfaceStyle == .dark ? dark : light
        return UIColor(
            red: rgba.red,
            green: rgba.green,
            blue: rgba.blue,
            alpha: rgba.alpha
        )
    }
}
#endif
