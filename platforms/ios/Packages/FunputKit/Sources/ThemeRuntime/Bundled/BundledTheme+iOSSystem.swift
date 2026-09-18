import ThemeSchema

public extension KeyboardTheme {
    /// A restrained system-style theme for users moving from Apple's keyboard.
    static let iosSystem = KeyboardTheme(
        id: "app.funput.theme.ios-system",
        metadata: ThemeMetadata(name: "iOS System", author: "Funput"),
        material: .translucent,
        palette: ThemePalette(
            backgroundStart: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xD1D3D9),
                dark: ThemeRGBA(hex: 0x1C1C1E)
            ),
            backgroundEnd: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xD1D3D9),
                dark: ThemeRGBA(hex: 0x1C1C1E)
            ),
            characterKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFFFF),
                dark: ThemeRGBA(hex: 0x464646)
            ),
            specialKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xABB1BA),
                dark: ThemeRGBA(hex: 0x38383A)
            ),
            border: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x000000, alpha: 0),
                dark: ThemeRGBA(hex: 0xFFFFFF, alpha: 0)
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
                light: ThemeRGBA(hex: 0x000000),
                dark: ThemeRGBA(hex: 0xFFFFFF)
            )
        ),
        metrics: ThemeMetrics(
            keyOpacity: 1,
            specialKeyOpacity: 1,
            cornerRadius: 8,
            borderWidth: 0,
            shadowOpacity: 0.24,
            shadowRadius: 1,
            pressedScale: 0.97,
            pressedOpacityMultiplier: 1.1,
            fontScale: 1
        )
    )
}
