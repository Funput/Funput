/// Render-ready theme values consumed directly by the keyboard renderer.
///
/// A `ResolvedTheme` is the output of `ThemeRuntime.resolve(_:context:)`: every
/// metric is already clamped to a safe range and every color is an adaptive
/// light/dark pair. The renderer never sees the authoring ``KeyboardTheme``.
public struct ResolvedTheme: Hashable, Sendable {
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
    public var keycapHeightScale: Double
    public var horizontalPadding: Double
    public var horizontalGap: Double
    public var verticalGap: Double
    public var colorEffects: ThemeColorEffects

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
        fontScale: Double,
        keycapHeightScale: Double = 1,
        horizontalPadding: Double = 6,
        horizontalGap: Double = 5,
        verticalGap: Double = 7,
        colorEffects: ThemeColorEffects? = nil
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
        self.keycapHeightScale = keycapHeightScale
        self.horizontalPadding = horizontalPadding
        self.horizontalGap = horizontalGap
        self.verticalGap = verticalGap
        self.colorEffects = colorEffects ?? ThemeColorEffects(pressedOverlay: accent)
    }
}
