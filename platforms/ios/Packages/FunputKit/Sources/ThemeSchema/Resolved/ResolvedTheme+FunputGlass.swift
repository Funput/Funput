public extension ResolvedTheme {
    /// The resolved default theme. Kept here as a plain value so the renderer's
    /// ``KeyboardPresentation`` has a dependency-free default; `ThemeRuntime`
    /// resolves the authored bundled theme to exactly these values (asserted by
    /// a parity test).
    static let funputGlass = ResolvedTheme(
        material: .glass,
        backgroundStart: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xDCE4ED, alpha: 0.58),
            dark: ThemeRGBA(hex: 0x3A3A3C, alpha: 0.48)
        ),
        backgroundEnd: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xAEBCCC, alpha: 0.44),
            dark: ThemeRGBA(hex: 0x121214, alpha: 0.62)
        ),
        characterKey: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xFFFFFF),
            dark: ThemeRGBA(hex: 0x6C6C70)
        ),
        specialKey: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0xAAB7C7),
            dark: ThemeRGBA(hex: 0x464649)
        ),
        border: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0x718096, alpha: 0.45),
            dark: ThemeRGBA(hex: 0xFFFFFF, alpha: 0.16)
        ),
        label: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0x111318),
            dark: ThemeRGBA(hex: 0xFFFFFF)
        ),
        secondaryLabel: AdaptiveThemeColor(
            light: ThemeRGBA(hex: 0x414957),
            dark: ThemeRGBA(hex: 0xEBEBF5, alpha: 0.82)
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
