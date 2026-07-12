import ThemeSchema

public extension KeyboardTheme {
    /// A deep, glassy dark theme with a violet accent.
    static let midnight = KeyboardTheme(
        id: "app.funput.theme.midnight",
        metadata: ThemeMetadata(name: "Midnight", author: "Funput"),
        material: .glass,
        palette: ThemePalette(
            backgroundStart: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x3A3A4C, alpha: 0.55),
                dark: ThemeRGBA(hex: 0x1A1A2E, alpha: 0.6)
            ),
            backgroundEnd: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x2A2A3C, alpha: 0.5),
                dark: ThemeRGBA(hex: 0x0A0A16, alpha: 0.66)
            ),
            characterKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x4A4A5C),
                dark: ThemeRGBA(hex: 0x2A2A3E)
            ),
            specialKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x3A3A4C),
                dark: ThemeRGBA(hex: 0x1E1E30)
            ),
            border: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFFFF, alpha: 0.16),
                dark: ThemeRGBA(hex: 0xFFFFFF, alpha: 0.12)
            ),
            label: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFFFF),
                dark: ThemeRGBA(hex: 0xFFFFFF)
            ),
            secondaryLabel: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xEBEBF5, alpha: 0.7),
                dark: ThemeRGBA(hex: 0xEBEBF5, alpha: 0.6)
            ),
            accent: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x7B61FF),
                dark: ThemeRGBA(hex: 0x9B81FF)
            )
        ),
        metrics: ThemeMetrics(
            keyOpacity: 0.8,
            specialKeyOpacity: 0.9,
            cornerRadius: 12,
            borderWidth: 0.5,
            shadowOpacity: 0.25,
            shadowRadius: 3,
            pressedScale: 0.95,
            pressedOpacityMultiplier: 1.2,
            fontScale: 1
        )
    )
}
