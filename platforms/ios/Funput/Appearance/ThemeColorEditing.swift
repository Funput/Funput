import SwiftUI
import ThemeSchema

enum ThemeColorRole: String, CaseIterable {
    case backgroundStart, backgroundEnd, characterKey, specialKey
    case label, secondaryLabel, accent, pressedOverlay

    var keyPath: WritableKeyPath<ThemePalette, AdaptiveThemeColor>? {
        switch self {
        case .backgroundStart: \ThemePalette.backgroundStart
        case .backgroundEnd: \ThemePalette.backgroundEnd
        case .characterKey: \ThemePalette.characterKey
        case .specialKey: \ThemePalette.specialKey
        case .label: \ThemePalette.label
        case .secondaryLabel: \ThemePalette.secondaryLabel
        case .accent: \ThemePalette.accent
        case .pressedOverlay: nil
        }
    }
}

extension ThemeEditorDraft {
    func color(for role: ThemeColorRole) -> Color {
        Color(rgba(for: role, in: customTheme.theme))
    }

    mutating func setColor(_ color: Color, for role: ThemeColorRole) {
        let old = rgba(for: role, in: customTheme.theme)
        let replacement = ThemeRGBA(color: color, preservingAlpha: old.alpha)
        if let keyPath = role.keyPath {
            var adaptive = customTheme.theme.palette[keyPath: keyPath]
            adaptive = adaptive.replacing(previewMode, with: replacement)
            customTheme.theme.palette[keyPath: keyPath] = adaptive
        } else {
            customTheme.theme.colorEffects.pressedOverlay = customTheme.theme
                .colorEffects.pressedOverlay.replacing(previewMode, with: replacement)
        }
        if role == .backgroundStart || role == .backgroundEnd {
            customTheme.theme.colorEffects.glassBackgroundTintEnabled = true
        }
        if role == .characterKey || role == .specialKey {
            customTheme.theme.colorEffects.glassKeyTintEnabled = true
        }
    }

    func hasChanged(_ roles: [ThemeColorRole]) -> Bool {
        roles.contains {
            rgba(for: $0, in: customTheme.theme) != rgba(for: $0, in: baseTheme)
        }
    }

    private func rgba(for role: ThemeColorRole, in theme: KeyboardTheme) -> ThemeRGBA {
        let adaptive = role.keyPath.map { theme.palette[keyPath: $0] }
            ?? theme.colorEffects.pressedOverlay
        return previewMode == .light ? adaptive.light : adaptive.dark
    }
}

private extension AdaptiveThemeColor {
    func replacing(_ mode: AppearancePreviewMode, with color: ThemeRGBA) -> Self {
        mode == .light
            ? AdaptiveThemeColor(light: color, dark: dark)
            : AdaptiveThemeColor(light: light, dark: color)
    }
}

private extension ThemeRGBA {
    init(color: Color, preservingAlpha alpha: Double) {
        let value = UIColor(color)
        var red: CGFloat = 0, green: CGFloat = 0, blue: CGFloat = 0
        value.getRed(&red, green: &green, blue: &blue, alpha: nil)
        self.init(red: red, green: green, blue: blue, alpha: alpha)
    }
}

private extension Color {
    init(_ rgba: ThemeRGBA) {
        self.init(.sRGB, red: rgba.red, green: rgba.green, blue: rgba.blue, opacity: 1)
    }
}
