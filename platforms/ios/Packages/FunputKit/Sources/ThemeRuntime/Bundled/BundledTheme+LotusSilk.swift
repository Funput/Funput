import ThemeSchema

public extension KeyboardTheme {
    /// A pearl-and-rose Liquid Glass theme inspired by lotus petals and silk.
    static let lotusSilk = FunputSignatureGlassTheme.make(
        id: "app.funput.theme.lotus-silk",
        name: "Lotus Silk",
        palette: ThemePalette(
            backgroundStart: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFF7F5, alpha: 0.88),
                dark: ThemeRGBA(hex: 0x3A172C, alpha: 0.68)
            ),
            backgroundEnd: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xEBC8CD, alpha: 0.76),
                dark: ThemeRGBA(hex: 0x160D18, alpha: 0.74)
            ),
            characterKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xFFFDFC),
                dark: ThemeRGBA(hex: 0x5B324C)
            ),
            specialKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xDCB0B8),
                dark: ThemeRGBA(hex: 0x422338)
            ),
            border: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x8A4C66, alpha: 0.28),
                dark: ThemeRGBA(hex: 0xFFD9E2, alpha: 0.18)
            ),
            label: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x512D46),
                dark: ThemeRGBA(hex: 0xFFF7FA)
            ),
            secondaryLabel: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x735266),
                dark: ThemeRGBA(hex: 0xE9C8D7)
            ),
            accent: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x8A571C),
                dark: ThemeRGBA(hex: 0xF1BE88)
            )
        ),
        gradientDirection: .diagonalLeft,
        cornerRadius: 8
    )
}
