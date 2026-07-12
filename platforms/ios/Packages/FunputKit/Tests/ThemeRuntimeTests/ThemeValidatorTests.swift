import Testing
import ThemeSchema
import ThemeRuntime

struct ThemeValidatorTests {
    @Test("Bundled themes pass validation", arguments: BundledThemes.all)
    func bundledThemesAreValid(_ theme: KeyboardTheme) {
        #expect(ThemeValidator.validate(theme).isEmpty)
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
