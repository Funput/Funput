import FunputShared
import Testing
@testable import Funput

@MainActor
@Suite("Toolbar visibility settings")
struct ToolbarVisibilitySettingsTests {
    @Test("Hiding the band switches off both features it carries")
    func hidingSwitchesFeaturesOff() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(store: store)
        #expect(model.showsToolbar)

        model.setToolbarVisible(false)

        #expect(!model.configuration.personalSuggestionsEnabled)
        #expect(!model.configuration.clipboardEnabled)
        #expect(!model.showsToolbar)
        // One save, not two: the keyboard reads the stored shape, never a half-applied one.
        #expect(!store.configuration.personalSuggestionsEnabled)
        #expect(!store.configuration.clipboardEnabled)
    }

    @Test("Showing the band brings both features back")
    func showingRestoresFeatures() {
        var configuration = FunputConfiguration.default
        configuration.personalSuggestionsEnabled = false
        configuration.clipboardEnabled = false
        let model = SettingsModel(store: SettingsTestStore(configuration: configuration))
        #expect(!model.showsToolbar)

        model.setToolbarVisible(true)

        #expect(model.configuration.personalSuggestionsEnabled)
        #expect(model.configuration.clipboardEnabled)
        #expect(model.showsToolbar)
    }

    @Test("The switch reads on while either feature is on")
    func eitherFeatureKeepsTheSwitchOn() {
        for (suggestions, clipboard) in [(true, false), (false, true)] {
            var configuration = FunputConfiguration.default
            configuration.personalSuggestionsEnabled = suggestions
            configuration.clipboardEnabled = clipboard
            let model = SettingsModel(store: SettingsTestStore(configuration: configuration))
            // Switching one feature off on its own leaves the band — and the switch —
            // alone; only the switch itself takes both down.
            #expect(model.showsToolbar)
        }
    }
}
