import FunputShared
import KeyboardLayout
import Testing
@testable import Funput

@MainActor
struct AdvancedTelexSettingsTests {
    @Test("Input method picker exposes the stable display order")
    func pickerOrder() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        let choices = SettingsPicker.inputMethod.choices(model: model)

        #expect(choices.map(\.id) == ["telex", "telex_advanced", "vni"])
        #expect(choices.map(\.title) == ["Telex", "Telex nâng cao", "VNI"])
        #expect(choices[1].summary == "Full Telex — [→ư, ]→ơ, w đầu từ→ư.")
    }

    @Test("Advanced Telex persists and leaves number row editable")
    func selection() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(store: store)
        let advanced = SettingsPicker.inputMethod.choices(model: model)[1]

        advanced.select()

        #expect(model.configuration.inputMethod == .telexAdvanced)
        #expect(store.configuration.inputMethod == .telexAdvanced)
        #expect(!model.isNumberRowLocked)
    }
}
