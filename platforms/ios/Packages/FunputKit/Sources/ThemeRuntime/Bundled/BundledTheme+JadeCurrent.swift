import ThemeSchema

public extension KeyboardTheme {
    /// A fresh jade-and-amber Liquid Glass theme with a calm, modern character.
    static let jadeCurrent = FunputSignatureGlassTheme.make(
        id: "app.funput.theme.jade-current",
        name: "Jade Current",
        palette: ThemePalette(
            backgroundStart: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xDDEFE9, alpha: 0.86),
                dark: ThemeRGBA(hex: 0x133638, alpha: 0.70)
            ),
            backgroundEnd: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x83B9A8, alpha: 0.72),
                dark: ThemeRGBA(hex: 0x081D20, alpha: 0.78)
            ),
            characterKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xF7FFFC),
                dark: ThemeRGBA(hex: 0x295653)
            ),
            specialKey: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0xA6CEC1),
                dark: ThemeRGBA(hex: 0x1B4140)
            ),
            border: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x255E58, alpha: 0.28),
                dark: ThemeRGBA(hex: 0xB9E9DB, alpha: 0.18)
            ),
            label: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x173C39),
                dark: ThemeRGBA(hex: 0xF4FFFC)
            ),
            secondaryLabel: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x3F625D),
                dark: ThemeRGBA(hex: 0xC0DDD6)
            ),
            accent: AdaptiveThemeColor(
                light: ThemeRGBA(hex: 0x755000),
                dark: ThemeRGBA(hex: 0xF2BE55)
            )
        ),
        gradientDirection: .horizontal,
        cornerRadius: 7
    )
}
