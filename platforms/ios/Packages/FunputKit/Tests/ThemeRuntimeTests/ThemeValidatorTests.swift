import Testing
import ThemeSchema
import ThemeRuntime

struct ThemeValidatorTests {
    @Test(
        "Signature themes pass every contrast check",
        arguments: [KeyboardTheme.lotusSilk, .jadeCurrent]
    )
    func signatureThemesAreValid(_ theme: KeyboardTheme) {
        #expect(ThemeValidator.validate(theme).isEmpty)
    }

    @Test("Bundled themes preserve readable primary and secondary labels", arguments: BundledThemes.all)
    func bundledLabelsAreValid(_ theme: KeyboardTheme) {
        let issues = ThemeValidator.validate(theme)
        #expect(!issues.contains { $0.message.hasPrefix("label/") })
        #expect(!issues.contains { $0.message.hasPrefix("secondary-label/") })
    }

    @Test("A low-contrast theme is flagged")
    func lowContrastFlagged() {
        let key = AdaptiveThemeColor(light: ThemeRGBA(hex: 0x808080), dark: ThemeRGBA(hex: 0x808080))
        let label = AdaptiveThemeColor(light: ThemeRGBA(hex: 0x888888), dark: ThemeRGBA(hex: 0x888888))
        let muddy = KeyboardTheme(
            id: "test.muddy",
            metadata: ThemeMetadata(name: "Muddy", author: "Test"),
            material: .solid,
            palette: ThemePalette(
                backgroundStart: key,
                backgroundEnd: key,
                characterKey: key,
                specialKey: key,
                border: key,
                label: label,
                secondaryLabel: label,
                accent: key
            ),
            metrics: BundledThemes.default.metrics
        )
        #expect(ThemeValidator.validate(muddy).contains { $0.kind == .lowContrast })
    }
}
