import Foundation

public enum KeyboardMaterial: String, Hashable, Sendable {
    case solid
    case translucent
    case glass
}

public struct ThemeRGBA: Hashable, Sendable {
    public let red: Double
    public let green: Double
    public let blue: Double
    public let alpha: Double

    public init(red: Double, green: Double, blue: Double, alpha: Double = 1) {
        self.red = red
        self.green = green
        self.blue = blue
        self.alpha = alpha
    }

    public init(hex: UInt32, alpha: Double = 1) {
        red = Double((hex >> 16) & 0xFF) / 255
        green = Double((hex >> 8) & 0xFF) / 255
        blue = Double(hex & 0xFF) / 255
        self.alpha = alpha
    }
}

public struct AdaptiveThemeColor: Hashable, Sendable {
    public let light: ThemeRGBA
    public let dark: ThemeRGBA

    public init(light: ThemeRGBA, dark: ThemeRGBA) {
        self.light = light
        self.dark = dark
    }
}

public struct KeyboardThemeTokens: Hashable, Sendable {
    public var material: KeyboardMaterial
    public var backgroundStart: AdaptiveThemeColor
    public var backgroundEnd: AdaptiveThemeColor
    public var characterKey: AdaptiveThemeColor
    public var specialKey: AdaptiveThemeColor
    public var border: AdaptiveThemeColor
    public var label: AdaptiveThemeColor
    public var secondaryLabel: AdaptiveThemeColor
    public var accent: AdaptiveThemeColor
    public var keyOpacity: Double
    public var specialKeyOpacity: Double
    public var cornerRadius: Double
    public var borderWidth: Double
    public var shadowOpacity: Double
    public var shadowRadius: Double
    public var pressedScale: Double
    public var pressedOpacityMultiplier: Double
    public var fontScale: Double

    public init(
        material: KeyboardMaterial,
        backgroundStart: AdaptiveThemeColor,
        backgroundEnd: AdaptiveThemeColor,
        characterKey: AdaptiveThemeColor,
        specialKey: AdaptiveThemeColor,
        border: AdaptiveThemeColor,
        label: AdaptiveThemeColor,
        secondaryLabel: AdaptiveThemeColor,
        accent: AdaptiveThemeColor,
        keyOpacity: Double,
        specialKeyOpacity: Double,
        cornerRadius: Double,
        borderWidth: Double,
        shadowOpacity: Double,
        shadowRadius: Double,
        pressedScale: Double,
        pressedOpacityMultiplier: Double,
        fontScale: Double
    ) {
        self.material = material
        self.backgroundStart = backgroundStart
        self.backgroundEnd = backgroundEnd
        self.characterKey = characterKey
        self.specialKey = specialKey
        self.border = border
        self.label = label
        self.secondaryLabel = secondaryLabel
        self.accent = accent
        self.keyOpacity = keyOpacity
        self.specialKeyOpacity = specialKeyOpacity
        self.cornerRadius = cornerRadius
        self.borderWidth = borderWidth
        self.shadowOpacity = shadowOpacity
        self.shadowRadius = shadowRadius
        self.pressedScale = pressedScale
        self.pressedOpacityMultiplier = pressedOpacityMultiplier
        self.fontScale = fontScale
    }

    public static let funputGlass = KeyboardThemeTokens(
        material: .glass,
        backgroundStart: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xCAD4E0, alpha: 0.98),
            dark: ThemeRGBA(hex: 0x151A26, alpha: 0.96)
        ),
        backgroundEnd: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xB8C5D4, alpha: 0.98),
            dark: ThemeRGBA(hex: 0x090D18, alpha: 0.98)
        ),
        characterKey: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xFFFFFF),
            dark: ThemeRGBA(hex: 0x263142)
        ),
        specialKey: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xAAB7C7),
            dark: ThemeRGBA(hex: 0x3A4658)
        ),
        border: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0x718096, alpha: 0.45),
            dark: ThemeRGBA(hex: 0x94A3B8, alpha: 0.36)
        ),
        label: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0x111318),
            dark: ThemeRGBA(hex: 0xF7F9FF)
        ),
        secondaryLabel: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0x414957),
            dark: ThemeRGBA(hex: 0xC8D0DE)
        ),
        accent: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xA98B32),
            dark: ThemeRGBA(hex: 0xC8A951)
        ),
        keyOpacity: 0.72,
        specialKeyOpacity: 0.82,
        cornerRadius: 10,
        borderWidth: 0.5,
        shadowOpacity: 0.16,
        shadowRadius: 2,
        pressedScale: 0.96,
        pressedOpacityMultiplier: 1.18,
        fontScale: 1
    )
}
