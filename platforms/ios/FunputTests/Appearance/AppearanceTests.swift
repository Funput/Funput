import FunputShared
import KeyboardLayout
import KeyboardRenderer
import SwiftUI
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct AppearanceTests {
    @Test("Unknown stored theme falls back to Funput Glass")
    func invalidThemeFallback() {
        var configuration = FunputConfiguration.default
        configuration.selectedThemeID = "missing"
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: configuration),
            customStore: ThemeTestStore()
        )

        #expect(model.appliedThemeID == FunputConfiguration.defaultThemeID)
        #expect(model.previewThemeID == FunputConfiguration.defaultThemeID)
    }

    @Test("Selecting previews without saving")
    func previewBeforeApply() {
        let store = SettingsTestStore(configuration: .default)
        let model = AppearanceModel(store: store, customStore: ThemeTestStore())

        model.selectTheme(KeyboardTheme.midnight.id)

        #expect(model.previewThemeID == KeyboardTheme.midnight.id)
        #expect(model.appliedThemeID == FunputConfiguration.defaultThemeID)
        #expect(store.saveCount == 0)
    }

    @Test("Apply persists only the selected theme")
    func applyTheme() {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .telex
        configuration.heightScale = 1.12
        let store = SettingsTestStore(configuration: configuration)
        let model = AppearanceModel(store: store, customStore: ThemeTestStore())

        model.selectTheme(KeyboardTheme.midnight.id)
        model.applyPreview()

        #expect(store.configuration.selectedThemeID == KeyboardTheme.midnight.id)
        #expect(store.configuration.inputMethod == .telex)
        #expect(store.configuration.heightScale == 1.12)
        #expect(model.isPreviewApplied)
    }

    @Test("Failed apply retains applied theme and draft")
    func failedApply() {
        let store = SettingsTestStore(configuration: .default, acceptsSaves: false)
        let model = AppearanceModel(store: store, customStore: ThemeTestStore())
        model.selectTheme(KeyboardTheme.midnight.id)

        model.applyPreview()

        #expect(model.appliedThemeID == FunputConfiguration.defaultThemeID)
        #expect(model.previewThemeID == KeyboardTheme.midnight.id)
        #expect(model.showsSaveError)
    }

    @Test("Reset changes only the theme")
    func resetTheme() {
        var configuration = FunputConfiguration.default
        configuration.selectedThemeID = KeyboardTheme.midnight.id
        configuration.smartRestore = false
        let store = SettingsTestStore(configuration: configuration)
        let model = AppearanceModel(store: store, customStore: ThemeTestStore())

        model.resetTheme()

        #expect(store.configuration.selectedThemeID == FunputConfiguration.defaultThemeID)
        #expect(!store.configuration.smartRestore)
    }

    @Test("Reload synchronizes themes without changing preview mode")
    func reload() {
        let store = SettingsTestStore(configuration: .default)
        let model = AppearanceModel(store: store, customStore: ThemeTestStore())
        model.previewMode = .dark
        store.configuration.selectedThemeID = KeyboardTheme.classicLight.id

        model.reload()

        #expect(model.appliedThemeID == KeyboardTheme.classicLight.id)
        #expect(model.previewThemeID == KeyboardTheme.classicLight.id)
        #expect(model.previewMode == .dark)
    }

    @Test("Catalog and preview use production themes")
    func catalogAndPreview() {
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore()
        )
        let ids = model.themes.map(\.id)

        #expect(ids == BundledThemes.all.map(\.id))
        #expect(Set(ids).count == 3)
        #expect(model.presentation(for: KeyboardTheme.midnight.id).theme != .funputGlass)
        #expect(AppearancePreviewMode.light.interfaceStyle == .light)
        #expect(AppearancePreviewMode.dark.interfaceStyle == .dark)
    }

    @Test("Keyboard preview interaction is opt-in")
    func previewInteraction() {
        let presentation = KeyboardPreviewPresentation.make(configuration: .default)
        #expect(!KeyboardPreview(presentation: presentation).isInteractive)
        #expect(KeyboardPreview(presentation: presentation, isInteractive: true).isInteractive)
    }
}
