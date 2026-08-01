import ThemeSchema

enum FunputSignatureGlassTheme {
    static func make(
        id: String,
        name: String,
        palette: ThemePalette,
        gradientDirection: ThemeGradientDirection,
        cornerRadius: Double
    ) -> KeyboardTheme {
        KeyboardTheme(
            id: id,
            metadata: ThemeMetadata(name: name, author: "Funput"),
            material: .glass,
            palette: palette,
            metrics: ThemeMetrics(
                keyOpacity: 0.78,
                specialKeyOpacity: 0.86,
                cornerRadius: cornerRadius,
                borderWidth: 0.6,
                shadowOpacity: 0.18,
                shadowRadius: 2.5,
                pressedScale: 0.96,
                pressedOpacityMultiplier: 1.16,
                fontScale: 1
            ),
            colorEffects: ThemeColorEffects(
                glassBackgroundTintEnabled: true,
                glassKeyTintEnabled: true,
                pressedOverlayEnabled: true,
                pressedOverlay: palette.accent
            ),
            surfaceEffects: ThemeSurfaceEffects(
                glassBorderOverrideEnabled: true,
                glassShadowOverrideEnabled: false
            ),
            gradientDirection: gradientDirection
        )
    }
}
