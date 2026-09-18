import FunputShared
import KeyboardLayout
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
@Suite("Key sizing theme selection")
struct KeySizingThemeSelectionTests {
    @Test("System sizing selects the iOS theme atomically")
    func systemSizingSelection() throws {
        let store = SettingsTestStore(configuration: .default)
        let bootstrap = KeyboardBootstrapTestStore()
        let model = SettingsModel(store: store, bootstrap: bootstrap)
        let choice = try #require(
            SettingsPicker.keySizing.choices(model: model).first { $0.id == "system" }
        )

        choice.select()

        #expect(model.configuration.keySizing == .system)
        #expect(model.configuration.selectedThemeID == KeyboardTheme.iosSystem.id)
        #expect(store.configuration == model.configuration)
        #expect(bootstrap.saves.last?.0 == model.configuration)
    }

    @Test("Manual themes remain selected until system sizing is chosen again")
    func manualThemeOverride() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(store: store)
        model.selectKeySizing(.system)

        let appearance = AppearanceModel(store: store, customStore: ThemeTestStore())
        appearance.selectTheme(KeyboardTheme.midnight.id)
        appearance.applyPreview()
        model.reload()
        model.update(\.language, to: .english)
        model.selectKeySizing(.funput)

        #expect(model.configuration.selectedThemeID == KeyboardTheme.midnight.id)

        model.selectKeySizing(.system)
        #expect(model.configuration.selectedThemeID == KeyboardTheme.iosSystem.id)
    }

    @Test("Failed bootstrap rolls sizing and theme back together")
    func bootstrapRollback() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(
            store: store,
            bootstrap: KeyboardBootstrapTestStore(acceptsSaves: false)
        )

        model.selectKeySizing(.system)

        #expect(model.configuration == .default)
        #expect(store.configuration == .default)
        #expect(model.showsSaveError)
    }
}
