import ThemeSchema

enum ThemeBackgroundStop: Equatable {
    case start
    case end
}

extension ThemeEditorDraft {
    func backgroundOpacity(for stop: ThemeBackgroundStop) -> Double {
        let color = backgroundColor(for: stop)
        return previewMode == .light ? color.light.alpha : color.dark.alpha
    }

    mutating func setBackgroundOpacity(_ value: Double, for stop: ThemeBackgroundStop) {
        let color = backgroundColor(for: stop)
        let current = previewMode == .light ? color.light : color.dark
        let replacement = ThemeRGBA(
            red: current.red,
            green: current.green,
            blue: current.blue,
            alpha: value
        )
        let updated = previewMode == .light
            ? AdaptiveThemeColor(light: replacement, dark: color.dark)
            : AdaptiveThemeColor(light: color.light, dark: replacement)
        switch stop {
        case .start: customTheme.theme.palette.backgroundStart = updated
        case .end: customTheme.theme.palette.backgroundEnd = updated
        }
        enableGlassBackgroundTint()
    }

    mutating func setGradientDirection(_ direction: ThemeGradientDirection) {
        customTheme.theme.gradientDirection = direction
        enableGlassBackgroundTint()
    }

    private func backgroundColor(for stop: ThemeBackgroundStop) -> AdaptiveThemeColor {
        switch stop {
        case .start: customTheme.theme.palette.backgroundStart
        case .end: customTheme.theme.palette.backgroundEnd
        }
    }

    private mutating func enableGlassBackgroundTint() {
        if customTheme.theme.material == .glass {
            customTheme.theme.colorEffects.glassBackgroundTintEnabled = true
        }
    }
}
