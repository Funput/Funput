import ThemeSchema

extension ThemeEditorDraft {
    var labelScale: Double { customTheme.theme.metrics.fontScale }

    mutating func setLabelScale(_ value: Double) {
        let range = ThemeMetrics.fontScaleRange
        customTheme.theme.metrics.fontScale = min(max(value, range.lowerBound), range.upperBound)
    }

    mutating func setLabelWeight(_ weight: ThemeLabelWeight) {
        customTheme.theme.labelWeight = weight
    }
}
