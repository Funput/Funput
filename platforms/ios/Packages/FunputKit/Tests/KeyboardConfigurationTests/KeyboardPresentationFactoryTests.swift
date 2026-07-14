#if canImport(UIKit)
import Testing
import UIKit
import FunputShared
import KeyboardLayout
import ThemeSchema
import ThemeRuntime
@testable import KeyboardConfiguration

@MainActor
struct KeyboardPresentationFactoryTests {
    @Test("Selected theme resolves into the presentation")
    func selectedThemeResolves() {
        var config = FunputConfiguration.default
        config.selectedThemeID = "app.funput.theme.midnight"
        #expect(KeyboardPresentationFactory.make(from: config).theme == resolved(.midnight))
    }

    @Test("Unknown theme id falls back to the bundled default")
    func unknownThemeFallsBack() {
        var config = FunputConfiguration.default
        config.selectedThemeID = "does.not.exist"
        #expect(KeyboardPresentationFactory.resolvedTheme(for: config) == resolved(BundledThemes.default))
    }

    @Test("Configuration flags map onto the presentation")
    func flagsMap() {
        var config = FunputConfiguration.default
        config.language = .english
        config.isHapticFeedbackEnabled = false
        config.showsKeyPreviews = false
        config.heightScale = 1.2
        let presentation = KeyboardPresentationFactory.make(from: config)
        #expect(presentation.language == .english)
        #expect(!presentation.isHapticFeedbackEnabled)
        #expect(!presentation.showsKeyPreviews)
        #expect(presentation.sizing.heightScale == 1.2)
    }

    @Test("Default configuration id matches the default bundled theme")
    func defaultThemeIdMatchesBundled() {
        #expect(FunputConfiguration.defaultThemeID == BundledThemes.default.id)
    }

    @Test("Custom geometry maps into presentation sizing")
    func customGeometryMapsToSizing() {
        var custom = CustomKeyboardTheme(baseTheme: .classicLight)
        custom.theme.geometry = ThemeGeometry(
            keycapHeightScale: 0.86,
            horizontalPadding: 14,
            horizontalGap: 3,
            verticalGap: 11
        )
        var config = FunputConfiguration.default
        config.selectedThemeID = custom.id

        let presentation = KeyboardPresentationFactory.make(
            from: config,
            catalog: ThemeCatalog(customThemes: [custom])
        )
        #expect(presentation.sizing.horizontalPadding == 14)
        #expect(presentation.sizing.horizontalGap == 3)
        #expect(presentation.sizing.verticalGap == 11)
        #expect(presentation.theme.keycapHeightScale == 0.86)
    }

    private func resolved(_ theme: KeyboardTheme) -> ResolvedTheme {
        ThemeRuntime.resolve(
            theme,
            context: ThemeResolveContext(reduceTransparency: UIAccessibility.isReduceTransparencyEnabled)
        )
    }
}
#endif
