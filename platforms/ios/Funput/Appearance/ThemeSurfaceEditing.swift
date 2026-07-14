import ThemeSchema

extension ThemeEditorDraft {
    mutating func setKeyOpacity(_ value: Double, special: Bool) {
        if special {
            customTheme.theme.metrics.specialKeyOpacity = value
        } else {
            customTheme.theme.metrics.keyOpacity = value
        }
        if customTheme.theme.material == .glass {
            customTheme.theme.colorEffects.glassKeyTintEnabled = true
        }
    }

    var borderOpacity: Double {
        let border = customTheme.theme.palette.border
        return previewMode == .light ? border.light.alpha : border.dark.alpha
    }

    mutating func setBorderOpacity(_ value: Double) {
        let border = customTheme.theme.palette.border
        let current = previewMode == .light ? border.light : border.dark
        let replacement = ThemeRGBA(
            red: current.red,
            green: current.green,
            blue: current.blue,
            alpha: value
        )
        customTheme.theme.palette.border = previewMode == .light
            ? AdaptiveThemeColor(light: replacement, dark: border.dark)
            : AdaptiveThemeColor(light: border.light, dark: replacement)
        enableGlassBorderOverride()
    }

    mutating func setBorderWidth(_ value: Double) {
        customTheme.theme.metrics.borderWidth = value
        enableGlassBorderOverride()
    }

    mutating func setShadowOpacity(_ value: Double) {
        customTheme.theme.metrics.shadowOpacity = value
        enableGlassShadowOverride()
    }

    mutating func setShadowRadius(_ value: Double) {
        customTheme.theme.metrics.shadowRadius = value
        enableGlassShadowOverride()
    }

    private mutating func enableGlassBorderOverride() {
        if customTheme.theme.material == .glass {
            customTheme.theme.surfaceEffects.glassBorderOverrideEnabled = true
        }
    }

    private mutating func enableGlassShadowOverride() {
        if customTheme.theme.material == .glass {
            customTheme.theme.surfaceEffects.glassShadowOverrideEnabled = true
        }
    }
}
