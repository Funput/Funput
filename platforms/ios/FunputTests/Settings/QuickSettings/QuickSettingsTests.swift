import FunputShared
import KeyboardLayout
import Testing
@testable import Funput

@MainActor
@Suite("Keyboard tab quick settings")
struct QuickSettingsTests {
    private func makeModel(
        _ configuration: FunputConfiguration = .default,
        fullAccess: Bool = true
    ) -> (SettingsModel, SettingsTestStore) {
        let store = SettingsTestStore(configuration: configuration)
        let access = KeyboardAccessStateTestStore(hasObservedFullAccess: fullAccess)
        return (SettingsModel(store: store, accessStore: access), store)
    }

    @Test("A tap flips the switch and saves it")
    func tapFlipsAndSaves() {
        let (model, store) = makeModel()
        let before = model.isOn(.autoCapitalize)

        #expect(model.toggle(.autoCapitalize) == nil)

        #expect(model.isOn(.autoCapitalize) == !before)
        #expect(store.configuration.autoCapitalize == !before)
        #expect(model.stateLabel(for: .autoCapitalize) == (before ? "Tắt" : "Bật"))
    }

    @Test("Haptics and sound ask for Full Access instead of switching on without it")
    func feedbackNeedsFullAccess() {
        var configuration = FunputConfiguration.default
        configuration.isHapticFeedbackEnabled = false
        configuration.isKeySoundEnabled = false
        let (model, store) = makeModel(configuration, fullAccess: false)

        #expect(model.toggle(.haptics) == .haptics)
        #expect(model.toggle(.sound) == .sound)
        #expect(!store.configuration.isHapticFeedbackEnabled)
        #expect(!store.configuration.isKeySoundEnabled)
    }

    @Test("Switching feedback off never needs Full Access")
    func feedbackOffWithoutAccess() {
        var configuration = FunputConfiguration.default
        configuration.isKeySoundEnabled = true
        let (model, store) = makeModel(configuration, fullAccess: false)

        #expect(model.toggle(.sound) == nil)
        #expect(!store.configuration.isKeySoundEnabled)
    }

    @Test("VNI keeps the number row on and locked")
    func vniLocksNumberRow() {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .vni
        configuration.showsNumberRow = false
        let (model, store) = makeModel(configuration)

        #expect(model.isOn(.numberRow))
        #expect(model.isLocked(.numberRow))
        #expect(model.stateLabel(for: .numberRow) == "Luôn bật")
        #expect(model.toggle(.numberRow) == nil)
        #expect(!store.configuration.showsNumberRow)
    }

    @Test("Telex lets the number row switch")
    func telexNumberRowSwitches() {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .telex
        configuration.showsNumberRow = false
        let (model, _) = makeModel(configuration)

        #expect(!model.isLocked(.numberRow))
        model.toggle(.numberRow)
        #expect(model.isOn(.numberRow))
    }

    @Test("Every tile edits a different setting")
    func tilesAreDistinct() {
        let keyPaths = QuickSetting.allCases.map(\.keyPath)
        #expect(Set(keyPaths).count == keyPaths.count)
        #expect(QuickSetting.allCases.count == 6)
    }

    @Test("The haptics tile remembers and names the chosen strength")
    func hapticsRemembersStrength() {
        var configuration = FunputConfiguration.default
        configuration.hapticFeedbackStrength = .strong
        let (model, store) = makeModel(configuration)

        #expect(model.stateLabel(for: .haptics) == "Tắt")
        #expect(model.toggle(.haptics) == nil)
        #expect(model.stateLabel(for: .haptics) == "Mạnh")
        #expect(model.toggle(.haptics) == nil)
        #expect(!store.configuration.isHapticFeedbackEnabled)
        #expect(store.configuration.hapticFeedbackStrength == .strong)
        #expect(model.toggle(.haptics) == nil)
        #expect(model.selectedHapticStrength == .strong)
    }
}
