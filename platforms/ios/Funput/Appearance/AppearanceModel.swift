import FunputShared
import KeyboardRenderer
import Observation
import SwiftUI
import ThemeRuntime
import ThemeSchema
import UIKit

@MainActor @Observable
final class AppearanceModel {
    private(set) var configuration: FunputConfiguration
    private(set) var appliedThemeID: String
    private(set) var customThemes: [CustomKeyboardTheme]
    var previewThemeID: String
    var previewMode = AppearancePreviewMode.light
    var showsSaveError = false

    private let store: any FunputConfigurationStoring
    let customStore: any CustomThemeStoring
    private var didSetInitialMode = false

    init(
        store: any FunputConfigurationStoring,
        customStore: any CustomThemeStoring = CustomThemeStore()
    ) {
        self.store = store
        self.customStore = customStore
        let loadedConfiguration = store.load()
        let loadedThemes = customStore.load()
        configuration = loadedConfiguration
        customThemes = loadedThemes
        let catalog = ThemeCatalog(customThemes: loadedThemes)
        let themeID = catalog.theme(id: loadedConfiguration.selectedThemeID)?.id
            ?? FunputConfiguration.defaultThemeID
        appliedThemeID = themeID
        previewThemeID = themeID
    }

    var catalog: ThemeCatalog { ThemeCatalog(customThemes: customThemes) }
    var themes: [KeyboardTheme] { catalog.all }
    var previewTheme: KeyboardTheme {
        catalog.theme(id: previewThemeID) ?? BundledThemes.default
    }
    var previewCustomTheme: CustomKeyboardTheme? {
        catalog.customTheme(id: previewThemeID)
    }
    var isPreviewApplied: Bool { previewThemeID == appliedThemeID }

    var previewPresentation: KeyboardPresentation {
        KeyboardPreviewPresentation.make(
            configuration: previewConfiguration,
            catalog: catalog
        )
    }

    func presentation(for themeID: String) -> KeyboardPresentation {
        var candidate = configuration
        candidate.selectedThemeID = validThemeID(themeID)
        candidate.heightScale = 1
        return KeyboardPreviewPresentation.make(configuration: candidate, catalog: catalog)
    }

    func selectTheme(_ id: String) { previewThemeID = validThemeID(id) }
    func applyPreview() { commit(themeID: previewThemeID) }
    func resetTheme() { commit(themeID: FunputConfiguration.defaultThemeID) }

    func reload() {
        configuration = store.load()
        customThemes = customStore.load()
        appliedThemeID = validThemeID(configuration.selectedThemeID)
        previewThemeID = appliedThemeID
    }

    func setInitialMode(for colorScheme: ColorScheme) {
        guard !didSetInitialMode else { return }
        previewMode = colorScheme == .dark ? .dark : .light
        didSetInitialMode = true
    }

    var previewModeBinding: Binding<AppearancePreviewMode> {
        Binding(get: { self.previewMode }, set: { self.previewMode = $0 })
    }

    var saveErrorBinding: Binding<Bool> {
        Binding(get: { self.showsSaveError }, set: { self.showsSaveError = $0 })
    }

    private var previewConfiguration: FunputConfiguration {
        var candidate = configuration
        candidate.selectedThemeID = previewThemeID
        return candidate
    }

    func commit(themeID: String) {
        var candidate = configuration
        candidate.selectedThemeID = validThemeID(themeID)
        guard store.save(candidate) else { showsSaveError = true; return }
        configuration = candidate
        appliedThemeID = candidate.selectedThemeID
        previewThemeID = candidate.selectedThemeID
    }

    func validThemeID(_ id: String) -> String {
        catalog.theme(id: id)?.id ?? FunputConfiguration.defaultThemeID
    }

    func refreshCustomThemes() {
        customThemes = customStore.load()
    }

    func replaceAppliedTheme(with themeID: String) -> Bool {
        var candidate = configuration
        candidate.selectedThemeID = themeID
        guard store.save(candidate) else {
            showsSaveError = true
            return false
        }
        configuration = candidate
        appliedThemeID = themeID
        return true
    }
}

enum AppearancePreviewMode: String, CaseIterable, Identifiable {
    case light
    case dark

    var id: String { rawValue }
    var title: String { self == .light ? "Sáng" : "Tối" }
    var interfaceStyle: UIUserInterfaceStyle { self == .light ? .light : .dark }
}
