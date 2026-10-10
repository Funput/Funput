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
    /// Whether the user pinned the keyboard's appearance instead of following the host app.
    public var pinsAppearance: Bool

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
        areSmartGesturesEnabled: Bool = true,
        pinsAppearance: Bool = false
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
        self.pinsAppearance = pinsAppearance
    }
}

public struct KeyboardKeyEvent: Sendable {
    public enum Phase: Equatable, Sendable {
        case pressed
        case repeated
        case swiped(KeySwipeAction)
        /// Caret movement from the spacebar trackpad, in characters.
        case cursorMoved(offset: Int)
        /// One word rubbed away by a leftward drag on Backspace.
        case deletedWord
        case alternateSelected(KeyAlternate)
        case released
        case cancelled
    }

    public let key: KeySpec
    public let phase: Phase
    /// Where the finger landed, for the phases that come from a real touch. Absent
    /// for keys raised by VoiceOver, key repeat, and the gesture phases, none of
    /// which have a lift point to describe.
    public let touch: KeyboardTouchEvidence?

    public init(key: KeySpec, phase: Phase, touch: KeyboardTouchEvidence? = nil) {
        self.key = key
        self.phase = phase
        self.touch = touch
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
