/// The numeric styling an authored theme defines.
///
/// Authoring values are unclamped; `ThemeRuntime` clamps them into safe ranges
/// during resolution so a malformed theme can never produce an unusable surface.
public struct ThemeMetrics: Codable, Hashable, Sendable {
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
}
