import FunputShared
import KeyboardLayout
import KeyboardRenderer
import SwiftUI
import ThemeSchema
import Testing
@testable import Funput

@MainActor
struct FunputTests {
    @Test("App shell exposes stable top-level tabs")
    func appTabs() {
        #expect(AppTab.allCases == [.settings, .appearance, .about])
        #expect(AppTab.defaultTab == .settings)
        #expect(Set(AppTab.allCases.map(\.title)).count == AppTab.allCases.count)
        #expect(AppTab.allCases.allSatisfy { !$0.title.isEmpty && !$0.systemImage.isEmpty })
    }

    @Test("Keyboard preview uses the production presentation factory")
    func keyboardPreviewPresentation() {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .vni
        configuration.selectedThemeID = FunputConfiguration.defaultThemeID

        let presentation = KeyboardPreviewPresentation.make(configuration: configuration)
        #expect(presentation.layout.inputMethod == .vni)
        #expect(!presentation.layout.rows.isEmpty)
        #expect(presentation.layout.toolbar?.systemInputModeKey == nil)
        #expect(presentation.theme == .funputGlass)

        configuration.showsGlobeKey = true
        let withGlobe = KeyboardPreviewPresentation.make(configuration: configuration)
        #expect(withGlobe.layout.toolbar?.systemInputModeKey?.role == .systemInputMode)
    }

    @Test("Version metadata presents the public version only")
    func versionMetadata() {
        let label = AppMetadata.versionLabel(from: [
            "CFBundleShortVersionString": "1.2.3",
            "CFBundleVersion": "45",
        ])
        #expect(label == "Phiên bản 1.2.3")
        #expect(AppMetadata.versionLabel(from: [:]) == "Phiên bản đang phát triển")
    }

    @Test("Settings persist successful changes")
    func settingsPersistence() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(store: store)

        model.update(\.inputMethod, to: .telex)
        model.update(\.language, to: .english)
        model.update(\.toneStyle, to: .modern)
        model.update(\.heightScale, to: 1.15)

        #expect(model.configuration.inputMethod == .telex)
        #expect(model.configuration.language == .english)
        #expect(model.configuration.toneStyle == .modern)
        #expect(model.configuration.heightScale == 1.15)
        #expect(store.configuration == model.configuration)
    }

    @Test("Settings height binding clamps to renderer limits")
    func settingsHeightLimits() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        model.heightBinding.wrappedValue = 2
        #expect(model.configuration.heightScale == 1.2)
        model.heightBinding.wrappedValue = 0
        #expect(model.configuration.heightScale == 0.85)
    }

    @Test("Number row preference is editable for Telex and locked on for VNI")
    func numberRowPreference() {
        var configuration = FunputConfiguration.default
        configuration.inputMethod = .telex
        let model = SettingsModel(store: SettingsTestStore(configuration: configuration))
        #expect(!model.isNumberRowLocked)
        #expect(!model.numberRowBinding.wrappedValue)
        model.numberRowBinding.wrappedValue = true
        #expect(model.configuration.showsNumberRow)

        model.update(\.inputMethod, to: .vni)
        #expect(model.isNumberRowLocked)
        #expect(model.numberRowBinding.wrappedValue)
        model.numberRowBinding.wrappedValue = false
        #expect(model.configuration.showsNumberRow)
    }

    @Test("Settings retain prior values when saving fails")
    func settingsSaveFailure() {
        let store = SettingsTestStore(configuration: .default, acceptsSaves: false)
        let model = SettingsModel(store: store)

        model.update(\.smartRestore, to: false)

        #expect(model.configuration.smartRestore)
        #expect(model.showsSaveError)
    }

    @Test("Settings reset and reload through storage")
    func settingsResetAndReload() {
        var custom = FunputConfiguration.default
        custom.inputMethod = .telex
        let store = SettingsTestStore(configuration: custom)
        let model = SettingsModel(store: store)

        model.reset()
        #expect(model.configuration == .default)

        store.configuration.language = .english
        model.reload()
        #expect(model.configuration.language == .english)
    }

    @Test("Settings picker metadata covers all choices")
    func settingsPickerMetadata() {
        let model = SettingsModel(store: SettingsTestStore(configuration: .default))
        let pickers: [SettingsPicker] = [.inputMethod, .language, .toneStyle]

        #expect(pickers.allSatisfy { !$0.title.isEmpty && !$0.summary.isEmpty && !$0.systemImage.isEmpty })
        #expect(SettingsPicker.inputMethod.choices(model: model).count == KeyboardInputMethod.allCases.count)
        #expect(SettingsPicker.language.choices(model: model).count == KeyboardLanguage.allCases.count)
        #expect(SettingsPicker.toneStyle.choices(model: model).count == ToneStyleOption.allCases.count)
    }
}

final class SettingsTestStore: FunputConfigurationStoring {
    var configuration: FunputConfiguration
    var acceptsSaves: Bool
    private(set) var saveCount = 0

    init(configuration: FunputConfiguration, acceptsSaves: Bool = true) {
        self.configuration = configuration
        self.acceptsSaves = acceptsSaves
    }

    func load() -> FunputConfiguration { configuration }

    func save(_ configuration: FunputConfiguration) -> Bool {
        saveCount += 1
        guard acceptsSaves else { return false }
        self.configuration = configuration
        return true
    }
}
