#if canImport(UIKit)
import KeyboardLayout
import ThemeSchema
import UIKit

public enum ShiftState: Hashable, Sendable {
    case lowercase
    case uppercase
    case capsLocked

    public var isUppercase: Bool { self != .lowercase }
}

public struct KeyboardPresentation: Hashable, Sendable {
    public var layout: KeyboardLayout
    public var sizing: KeyboardSizingProfile
    public var theme: KeyboardThemeTokens
    public var shiftState: ShiftState
    public var showsInputModeKey: Bool

    public init(
        layout: KeyboardLayout = .funputQWERTY,
        sizing: KeyboardSizingProfile = .default,
        theme: KeyboardThemeTokens = .funputGlass,
        shiftState: ShiftState = .lowercase,
        showsInputModeKey: Bool = true
    ) {
        self.layout = layout
        self.sizing = sizing
        self.theme = theme
        self.shiftState = shiftState
        self.showsInputModeKey = showsInputModeKey
    }
}

public struct KeyboardKeyEvent: Sendable {
    public enum Phase: Sendable {
        case pressed
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
        let baseHeight: CGFloat
        if traits.userInterfaceIdiom == .pad {
            baseHeight = padBaseHeight
        } else if traits.verticalSizeClass == .compact {
            baseHeight = phoneLandscapeBaseHeight
        } else {
            baseHeight = phonePortraitBaseHeight
        }
        return baseHeight * min(max(scale, 0.85), 1.15)
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
