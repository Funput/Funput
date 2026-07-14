import ThemeSchema

public extension KeyboardTheme {
    /// An opaque, system-style theme for users who prefer no translucency.
    static let classicLight = KeyboardTheme(
        id: "app.funput.theme.classic-light",
        metadata: ThemeMetadata(name: "Classic", author: "Funput"),
        material: .translucent,
        palette: ThemePalette(
            backgroundStart: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xF2F2F7),
                dark: ThemeRGBA(hex: 0x1C1C1E)
            ),
            backgroundEnd: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xE5E5EA),
                dark: ThemeRGBA(hex: 0x000000)
            ),
            characterKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFFFF),
                dark: ThemeRGBA(hex: 0x2C2C2E)
            ),
            specialKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xC7C7CC),
                dark: ThemeRGBA(hex: 0x3A3A3C)
            ),
            border: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x000000, alpha: 0.12),
                dark: ThemeRGBA(hex: 0xFFFFFF, alpha: 0.12)
            ),
            label: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x000000),
                dark: ThemeRGBA(hex: 0xFFFFFF)
            ),
            secondaryLabel: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x3C3C43, alpha: 0.6),
                dark: ThemeRGBA(hex: 0xEBEBF5, alpha: 0.6)
            ),
            accent: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x007AFF),
                dark: ThemeRGBA(hex: 0x0A84FF)
            )
        ),
        metrics: ThemeMetrics(
            keyOpacity: 1,
            specialKeyOpacity: 1,
            cornerRadius: 6,
            borderWidth: 0.5,
            shadowOpacity: 0.2,
            shadowRadius: 1,
            pressedScale: 0.97,
            pressedOpacityMultiplier: 1.1,
            fontScale: 1
        )
    )
}
