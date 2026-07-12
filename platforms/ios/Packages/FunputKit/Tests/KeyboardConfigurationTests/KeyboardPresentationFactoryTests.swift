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

    private func resolved(_ theme: KeyboardTheme) -> ResolvedTheme {
        ThemeRuntime.resolve(
            theme,
            context: ThemeResolveContext(reduceTransparency: UIAccessibility.isReduceTransparencyEnabled)
        )
    }
}
#endif
