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

    let store: any FunputConfigurationStoring
    let bootstrap: any KeyboardBootstrapSynchronizing
    let customStore: any CustomThemeStoring
    let assetStore: any ThemeAssetStoring
    private var didSetInitialMode = false
    private var imageDataCache: [String: Data] = [:]

    init(
        store: any FunputConfigurationStoring,
        customStore: any CustomThemeStoring = CustomThemeStore(),
        assetStore: any ThemeAssetStoring = PreviewThemeAssetStore(),
        bootstrap: any KeyboardBootstrapSynchronizing = NoopKeyboardBootstrapSynchronizer()
    ) {
        self.store = store
        self.customStore = customStore
        self.assetStore = assetStore
        self.bootstrap = bootstrap
        let loadedConfiguration = store.load()
        let loadedThemes = customStore.load()
        configuration = loadedConfiguration
        customThemes = loadedThemes
        let catalog = ThemeCatalog(customThemes: loadedThemes)
        let themeID = catalog.theme(id: loadedConfiguration.selectedThemeID)?.id
            ?? FunputConfiguration.defaultThemeID
        appliedThemeID = themeID
        previewThemeID = themeID
        cleanupAssets()
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
        candidate.heightScale = FunputConfiguration.default.heightScale
        return KeyboardPreviewPresentation.make(configuration: candidate, catalog: catalog)
    }

    func selectTheme(_ id: String) { previewThemeID = validThemeID(id) }
    func applyPreview() { commit(themeID: previewThemeID) }
    func resetTheme() { commit(themeID: FunputConfiguration.defaultThemeID) }

    func reload() {
        imageDataCache.removeAll()
        configuration = store.load()
        customThemes = customStore.load()
        appliedThemeID = validThemeID(configuration.selectedThemeID)
        previewThemeID = appliedThemeID
    }

    /// Seeds the preview from the appearance the keyboard will actually render in, which
    /// is the pinned one when the user pinned it and the system's otherwise. It stays a
    /// free toggle: the theme editor uses it to pick which half of each color pair to edit.
    func setInitialMode(for colorScheme: ColorScheme) {
        guard !didSetInitialMode else { return }
        switch configuration.keyboardAppearance {
        case .light: previewMode = .light
        case .dark: previewMode = .dark
        case .system: previewMode = colorScheme == .dark ? .dark : .light
        }
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

    func validThemeID(_ id: String) -> String {
        catalog.theme(id: id)?.id ?? FunputConfiguration.defaultThemeID
    }

    func refreshCustomThemes() {
        customThemes = customStore.load()
        cleanupAssets()
    }

    func acceptPersistedConfiguration(
        _ candidate: FunputConfiguration,
        updatesPreview: Bool
    ) {
        configuration = candidate
        appliedThemeID = candidate.selectedThemeID
        if updatesPreview { previewThemeID = candidate.selectedThemeID }
    }

    func imageData(for theme: KeyboardTheme) -> Data? {
        guard let id = theme.backgroundEffects.image?.assetID else { return nil }
        if let cached = imageDataCache[id] { return cached }
        let data = assetStore.renderedData(for: id)
        guard let data, UIImage(data: data) != nil else { return nil }
        imageDataCache[id] = data
        return data
    }

    private func cleanupAssets() {
        let ids = Set(customThemes.compactMap { $0.theme.backgroundEffects.image?.assetID })
        assetStore.cleanup(referencedAssetIDs: ids)
    }
}
enum AppearancePreviewMode: String, CaseIterable, Identifiable {
    case light
    case dark

    var id: String { rawValue }
    var title: String { self == .light ? "Sáng" : "Tối" }
    var interfaceStyle: UIUserInterfaceStyle { self == .light ? .light : .dark }
}
