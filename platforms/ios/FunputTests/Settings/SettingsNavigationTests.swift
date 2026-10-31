import FunputShared
import KeyboardLayout
import Testing
@testable import Funput

@MainActor
@Suite("Settings pages and their summaries")
struct SettingsNavigationTests {
    @Test("Every page is reachable from exactly one tab")
    func everyPageHasOneHome() {
        let hub = SettingsDestination.hubRows
        #expect(Set(hub).count == hub.count)
        // "Dữ liệu và khôi phục" lives under "Tôi", and "Từ điển cá nhân" inside
        // "Thông minh"; everything else is a row of "Bàn phím".
        let nested: Set<SettingsDestination> = [.data, .personalDictionary]
        #expect(Set(hub).union(nested) == Set(SettingsDestination.allCases))
        #expect(nested.isDisjoint(with: hub))
        #expect(Set(SettingsDestination.allCases.map(\.title)).count == SettingsDestination.allCases.count)
    }

    @Test("Smart input summary counts its switches in words")
    func smartInputSummary() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        let total = SmartInputToggle.all.count

        model.update { configuration in
            for toggle in SmartInputToggle.all { configuration[keyPath: toggle.keyPath] = true }
        }
        #expect(model.summary(for: .smartInput) == "Đang bật tất cả")

        model.update(SmartInputToggle.all[0].keyPath, to: false)
        #expect(model.summary(for: .smartInput) == "Đang bật \(total - 1) trên \(total) tính năng")

        model.update { configuration in
            for toggle in SmartInputToggle.all { configuration[keyPath: toggle.keyPath] = false }
        }
        #expect(model.summary(for: .smartInput) == "Đang tắt tất cả")
    }

    @Test("The smart input page lists every switch once")
    func smartInputTogglesAreDistinct() {
        let keyPaths = SmartInputToggle.all.map(\.keyPath)
        #expect(Set(keyPaths).count == keyPaths.count)
        #expect(Set(SmartInputToggle.all.map(\.id)).count == SmartInputToggle.all.count)
    }

    @Test("Typing summary names the tone style and what the space bar does")
    func typingSummary() {
        var configuration = FunputConfiguration.default
        configuration.toneStyle = .modern
        configuration.languageToggleEnabled = true
        let model = SettingsModel(store: SettingsTestStore(configuration: configuration))
        #expect(model.summary(for: .typing) == "Đặt dấu kiểu mới, đổi ngôn ngữ ở phím cách")

        model.update(\.toneStyle, to: .traditional)
        model.update(\.languageToggleEnabled, to: false)
        #expect(model.summary(for: .typing) == "Đặt dấu kiểu cũ, luôn gõ tiếng Việt")
    }

    @Test("Layout summary names the layout and the height")
    func layoutSummary() {
        var configuration = FunputConfiguration.default
        configuration.layoutPreset = .funput
        configuration.keySizing = .funput
        configuration.heightScale = 1.05
        let model = SettingsModel(store: SettingsTestStore(configuration: configuration))
        #expect(model.summary(for: .layout) == "Bố cục Funput, cao 105%")

        model.update(\.keySizing, to: .system)
        #expect(model.summary(for: .layout) == "Bố cục Funput, kích thước như iOS")
    }

    @Test("Feedback lists what is on; clipboard says how long it keeps")
    func feedbackAndClipboardSummaries() {
        var configuration = FunputConfiguration.default
        configuration.isHapticFeedbackEnabled = false
        configuration.isKeySoundEnabled = false
        configuration.showsKeyPreviews = false
        configuration.clipboardEnabled = false
        let model = SettingsModel(store: SettingsTestStore(configuration: configuration))
        #expect(model.summary(for: .feedback) == "Đang tắt tất cả")
        #expect(model.summary(for: .clipboard) == "Không lưu lịch sử")

        model.update(\.showsKeyPreviews, to: true)
        #expect(model.summary(for: .feedback) == "Đang bật xem trước phím")
        model.update(\.isHapticFeedbackEnabled, to: true)
        model.update(\.isKeySoundEnabled, to: true)
        #expect(model.summary(for: .feedback) == "Đang bật rung, âm thanh và xem trước phím")

        model.update(\.clipboardEnabled, to: true)
        #expect(model.summary(for: .clipboard) == "Lưu lịch sử trong \(model.clipboardExpiryLabel)")
    }

    @Test("No summary separates its parts with a dot or a dash")
    func summariesReadAsSentences() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        for destination in SettingsDestination.allCases {
            let summary = model.summary(for: destination) ?? ""
            #expect(!summary.contains("·") && !summary.contains("—") && !summary.contains("–"))
        }
    }

    @Test("The data page shows no summary")
    func dataHasNoSummary() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        #expect(model.summary(for: .data) == nil)
    }
}
