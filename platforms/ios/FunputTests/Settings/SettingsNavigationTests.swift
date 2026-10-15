import FunputShared
import Testing
@testable import Funput

@MainActor
@Suite("Settings pages and their summaries")
struct SettingsNavigationTests {
    @Test("Every page is reachable from exactly one tab")
    func everyPageHasOneHome() {
        let hub = SettingsDestination.hubRows
        #expect(Set(hub).count == hub.count)
        // "Dữ liệu và khôi phục" lives under "Tôi"; everything else under "Bàn phím".
        #expect(Set(hub).union([.data]) == Set(SettingsDestination.allCases))
        #expect(!hub.contains(.data))
        #expect(Set(SettingsDestination.allCases.map(\.title)).count == SettingsDestination.allCases.count)
    }

    @Test("Smart input summary counts its switches")
    func smartInputSummary() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        let total = SmartInputToggle.all.count
        let enabled = SmartInputToggle.all.filter { FunputConfiguration.default[keyPath: $0.keyPath] }.count
        #expect(model.summary(for: .smartInput) == "\(enabled)/\(total) bật")

        model.update { configuration in
            for toggle in SmartInputToggle.all { configuration[keyPath: toggle.keyPath] = false }
        }
        #expect(model.summary(for: .smartInput) == "0/\(total) bật")
    }

    @Test("The smart input page lists every switch once")
    func smartInputTogglesAreDistinct() {
        let keyPaths = SmartInputToggle.all.map(\.keyPath)
        #expect(Set(keyPaths).count == keyPaths.count)
        #expect(Set(SmartInputToggle.all.map(\.id)).count == SmartInputToggle.all.count)
    }

    @Test("Typing summary names the method and the tone style")
    func typingSummary() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        #expect(model.summary(for: .typing) == "\(model.inputMethodLabel) · \(model.toneStyleLabel)")
    }

    @Test("Feedback and clipboard read off when switched off")
    func offSummaries() {
        var configuration = FunputConfiguration.default
        configuration.isHapticFeedbackEnabled = false
        configuration.isKeySoundEnabled = false
        configuration.showsKeyPreviews = false
        configuration.clipboardEnabled = false
        let model = SettingsModel(store: SettingsTestStore(configuration: configuration))
        #expect(model.summary(for: .feedback) == "Tắt")
        #expect(model.summary(for: .clipboard) == "Tắt")

        model.update(\.clipboardEnabled, to: true)
        model.update(\.showsKeyPreviews, to: true)
        #expect(model.summary(for: .clipboard) == model.clipboardExpiryLabel)
        #expect(model.summary(for: .feedback) == "1/3 bật")
    }

    @Test("The data page shows no summary")
    func dataHasNoSummary() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        #expect(model.summary(for: .data) == nil)
    }
}
