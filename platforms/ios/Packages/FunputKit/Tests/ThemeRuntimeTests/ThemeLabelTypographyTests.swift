import Foundation
import Testing
import ThemeRuntime
import ThemeSchema

/// The key label size and weight a custom theme can set (issue #499, Android #551).
@Suite("Theme label typography")
struct ThemeLabelTypographyTests {
    @Test("Schema v6 themes keep the regular weight they were drawn with")
    func migratesV6LabelWeight() throws {
        let encoded = try JSONEncoder().encode(BundledThemes.default)
        var object = try #require(JSONSerialization.jsonObject(with: encoded) as? [String: Any])
        object["schemaVersion"] = 6
        object.removeValue(forKey: "labelWeight")
        let data = try JSONSerialization.data(withJSONObject: object)

        let decoded = try JSONDecoder().decode(KeyboardTheme.self, from: data)
        #expect(decoded.labelWeight == .regular)
    }

    @Test("A custom theme's label size and weight survive a JSON round-trip")
    func roundTrip() throws {
        var custom = CustomKeyboardTheme(baseTheme: .midnight)
        custom.theme.metrics.fontScale = 1.2
        custom.theme.labelWeight = .bold

        let data = try JSONEncoder().encode(custom)
        #expect(try JSONDecoder().decode(CustomKeyboardTheme.self, from: data) == custom)
    }

    @Test("Resolution carries the weight and holds the size to the editor's range")
    func resolves() {
        var theme = BundledThemes.default
        theme.labelWeight = .light
        theme.metrics.fontScale = 2

        let resolved = ThemeRuntime.resolve(theme)
        #expect(resolved.labelWeight == .light)
        #expect(resolved.fontScale == ThemeMetrics.fontScaleRange.upperBound)

        theme.metrics.fontScale = 0.1
        #expect(ThemeRuntime.resolve(theme).fontScale == ThemeMetrics.fontScaleRange.lowerBound)
    }
}
