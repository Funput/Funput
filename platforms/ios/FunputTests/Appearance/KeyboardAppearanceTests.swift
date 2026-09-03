import FunputShared
import SwiftUI
import Testing
@testable import Funput

@MainActor
struct KeyboardAppearanceTests {
    @Test("Choosing an appearance persists it without touching the theme")
    func commitPersists() {
        let store = SettingsTestStore(configuration: .default)
        let model = AppearanceModel(store: store, customStore: ThemeTestStore())

        model.commit(keyboardAppearance: .dark)

        #expect(model.configuration.keyboardAppearance == .dark)
        #expect(store.configuration.keyboardAppearance == .dark)
        #expect(model.appliedThemeID == FunputConfiguration.defaultThemeID)
        #expect(!model.showsSaveError)
    }

    @Test("A failed save keeps the previous appearance and reports the error")
    func commitFailureKeepsPrevious() {
        var configuration = FunputConfiguration.default
        configuration.keyboardAppearance = .light
        let store = SettingsTestStore(configuration: configuration, acceptsSaves: false)
        let model = AppearanceModel(store: store, customStore: ThemeTestStore())

        model.commit(keyboardAppearance: .dark)

        #expect(model.configuration.keyboardAppearance == .light)
        #expect(model.showsSaveError)
    }

    /// The picker reads straight off the stored configuration, so a rejected write has to
    /// leave the binding showing what is actually saved.
    @Test("The picker binding follows the stored configuration")
    func bindingReflectsConfiguration() {
        var configuration = FunputConfiguration.default
        configuration.keyboardAppearance = .dark
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: configuration),
            customStore: ThemeTestStore()
        )

        #expect(model.keyboardAppearanceBinding.wrappedValue == .dark)
        model.keyboardAppearanceBinding.wrappedValue = .light
        #expect(model.keyboardAppearanceBinding.wrappedValue == .light)
    }

    @Test("A pinned appearance seeds the preview instead of the system scheme")
    func pinnedAppearanceSeedsPreview() {
        var configuration = FunputConfiguration.default
        configuration.keyboardAppearance = .dark
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: configuration),
            customStore: ThemeTestStore()
        )

        model.setInitialMode(for: .light)

        #expect(model.previewMode == .dark)
    }

    @Test("Following the host app leaves the preview on the system scheme")
    func systemAppearanceSeedsFromColorScheme() {
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: ThemeTestStore()
        )

        model.setInitialMode(for: .dark)

        #expect(model.previewMode == .dark)
    }
}
