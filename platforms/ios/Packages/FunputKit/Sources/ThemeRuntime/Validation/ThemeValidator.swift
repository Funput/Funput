import ThemeSchema

/// Static checks that flag authored themes likely to render poorly.
///
/// The validator never mutates a theme; ``ThemeRuntime/resolve(_:context:)``
/// already clamps metrics. It only reports issues so authoring UIs can warn.
public enum ThemeValidator {
    /// Minimum label-vs-key contrast considered legible. `3.0` matches WCAG AA
    /// for large text, which key captions approximate.
    public static let minimumLabelContrast = 3.0

    public static func validate(_ theme: KeyboardTheme) -> [ThemeIssue] {
        contrastIssues(theme.palette)
    }

    private static func contrastIssues(_ palette: ThemePalette) -> [ThemeIssue] {
        let pairs: [(String, AdaptiveThemeColor, AdaptiveThemeColor)] = [
            ("label/character-key", palette.label, palette.characterKey),
            ("label/special-key", palette.label, palette.specialKey),
            ("secondary-label/character-key", palette.secondaryLabel, palette.characterKey),
            ("accent/special-key", palette.accent, palette.specialKey),
        ]
        return pairs.flatMap { role, foreground, background in
            [("light", foreground.light, background.light),
             ("dark", foreground.dark, background.dark)].compactMap { mode, text, key in
                guard ContrastRatio.between(text, key) < minimumLabelContrast else { return nil }
                return ThemeIssue(
                    kind: .lowContrast,
                    message: "\(role) contrast is too low in the \(mode) appearance."
                )
            }
        }
    }
}
