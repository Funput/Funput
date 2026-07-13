import ThemeSchema

public extension KeyboardTheme {
    /// A deep, glassy dark theme with a violet accent.
    static let midnight = KeyboardTheme(
        id: "app.funput.theme.midnight",
        metadata: ThemeMetadata(name: "Midnight", author: "Funput"),
        material: .glass,
        palette: ThemePalette(
            backgroundStart: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xE9E7F5, alpha: 0.86),
                dark: ThemeRGBA(hex: 0x1A1A2E, alpha: 0.6)
            ),
            backgroundEnd: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xD4D0EA, alpha: 0.78),
                dark: ThemeRGBA(hex: 0x0A0A16, alpha: 0.66)
            ),
            characterKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFFFF),
                dark: ThemeRGBA(hex: 0x2A2A3E)
            ),
            specialKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xC9C4DF),
                dark: ThemeRGBA(hex: 0x1E1E30)
            ),
            border: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x4B456C, alpha: 0.24),
                dark: ThemeRGBA(hex: 0xFFFFFF, alpha: 0.12)
            ),
            label: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x171426),
                dark: ThemeRGBA(hex: 0xFFFFFF)
            ),
            secondaryLabel: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x4B4564, alpha: 0.78),
                dark: ThemeRGBA(hex: 0xEBEBF5, alpha: 0.6)
            ),
            accent: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x5B4BC4),
                dark: ThemeRGBA(hex: 0x9B81FF)
            )
        ),
        metrics: ThemeMetrics(
            keyOpacity: 0.8,
            specialKeyOpacity: 0.9,
            cornerRadius: 6,
            borderWidth: 0.5,
            shadowOpacity: 0.25,
            shadowRadius: 3,
            pressedScale: 0.95,
            pressedOpacityMultiplier: 1.2,
            fontScale: 1
        )
    )
}
