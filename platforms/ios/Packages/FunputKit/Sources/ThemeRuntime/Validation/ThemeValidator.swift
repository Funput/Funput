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
        let appearances: [(String, ThemeRGBA, ThemeRGBA)] = [
            ("light", palette.label.light, palette.characterKey.light),
            ("dark", palette.label.dark, palette.characterKey.dark),
        ]
        return appearances.compactMap { appearance, label, key in
            guard ContrastRatio.between(label, key) < minimumLabelContrast else { return nil }
            return ThemeIssue(
                kind: .lowContrast,
                message: "Label vs character-key contrast is too low in the \(appearance) appearance."
            )
        }
    }
}
