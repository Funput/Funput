import Foundation
import KeyboardLayout
import Testing
import ThemeRuntime
import ThemeSchema

@Suite("iOS system theme")
struct iOSSystemThemeTests {
    @Test("Theme is bundled in the intended order with system styling")
    func bundledDefinition() {
        let theme = KeyboardTheme.iosSystem

        #expect(BundledThemes.all[1] == theme)
        #expect(theme.id == "app.funput.theme.ios-system")
        #expect(theme.material == .translucent)
        #expect(theme.metrics.cornerRadius == 8)
        #expect(theme.metrics.borderWidth == 0)
        #expect(theme.palette.accent == theme.palette.label)
    }

    @Test("Theme is valid and survives persistence")
    func validationAndPersistence() throws {
        let theme = KeyboardTheme.iosSystem
        let data = try JSONEncoder().encode(theme)

        #expect(ThemeValidator.validate(theme).isEmpty)
        #expect(try JSONDecoder().decode(KeyboardTheme.self, from: data) == theme)
    }

    @Test("System sizing retains the measured 390-point geometry")
    func systemGeometry() {
        #expect(KeyboardSizingProfile.system.verticalGap == 11)
        #expect(SystemKeyMetrics.rowsHeight(screenWidth: 390, rowCount: 4) / 4 == 43)
    }
}
