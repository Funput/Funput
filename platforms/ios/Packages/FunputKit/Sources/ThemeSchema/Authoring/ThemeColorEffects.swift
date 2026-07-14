/// Optional color effects that preserve bundled Liquid Glass behavior by default.
public struct ThemeColorEffects: Codable, Hashable, Sendable {
    public var glassBackgroundTintEnabled: Bool
    public var glassKeyTintEnabled: Bool
    public var pressedOverlayEnabled: Bool
    public var pressedOverlay: AdaptiveThemeColor

    public init(
        glassBackgroundTintEnabled: Bool = false,
        glassKeyTintEnabled: Bool = false,
        pressedOverlayEnabled: Bool = false,
        pressedOverlay: AdaptiveThemeColor
    ) {
        self.glassBackgroundTintEnabled = glassBackgroundTintEnabled
        self.glassKeyTintEnabled = glassKeyTintEnabled
        self.pressedOverlayEnabled = pressedOverlayEnabled
        self.pressedOverlay = pressedOverlay
    }
}
