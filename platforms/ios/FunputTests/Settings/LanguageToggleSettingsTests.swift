import FunputShared
import KeyboardLayout
import Testing
@testable import Funput

@MainActor
@Suite("Language toggle settings")
struct LanguageToggleSettingsTests {
    @Test("Removing the switch leaves the keyboard in Vietnamese")
    func removingTheSwitchResetsLanguage() {
        var configuration = FunputConfiguration.default
        configuration.language = .english
        let store = SettingsTestStore(configuration: configuration)
        let model = SettingsModel(store: store)

        model.setLanguageToggle(false)

        // Both in one save: English behind a switch that no longer exists would leave no
        // way back to Vietnamese from the keyboard.
        #expect(model.configuration.language == .vietnamese)
        #expect(!model.configuration.languageToggleEnabled)
        #expect(store.configuration.language == .vietnamese)
        #expect(model.isLanguageLocked)
    }

    @Test("Putting the switch back leaves the language alone")
    func restoringTheSwitchKeepsLanguage() {
        var configuration = FunputConfiguration.default
        configuration.languageToggleEnabled = false
        let model = SettingsModel(store: SettingsTestStore(configuration: configuration))

        model.setLanguageToggle(true)

        #expect(model.configuration.languageToggleEnabled)
        #expect(model.configuration.language == .vietnamese)
        #expect(!model.isLanguageLocked)
    }
}
