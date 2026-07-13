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
    var previewThemeID: String
    var previewMode = AppearancePreviewMode.light
    var showsSaveError = false

    private let store: any FunputConfigurationStoring
    private var didSetInitialMode = false

    init(store: any FunputConfigurationStoring) {
        self.store = store
        let configuration = store.load()
        let themeID = Self.validThemeID(configuration.selectedThemeID)
        self.configuration = configuration
        appliedThemeID = themeID
        previewThemeID = themeID
    }

    var themes: [KeyboardTheme] { BundledThemes.all }
    var previewTheme: KeyboardTheme { BundledThemes.theme(id: previewThemeID) ?? BundledThemes.default }
    var isPreviewApplied: Bool { previewThemeID == appliedThemeID }

    var previewPresentation: KeyboardPresentation {
        KeyboardPreviewPresentation.make(configuration: previewConfiguration)
    }

    func presentation(for themeID: String) -> KeyboardPresentation {
        var candidate = configuration
        candidate.selectedThemeID = Self.validThemeID(themeID)
        candidate.heightScale = 1
        return KeyboardPreviewPresentation.make(configuration: candidate)
    }

    func selectTheme(_ id: String) {
        previewThemeID = Self.validThemeID(id)
    }

    func applyPreview() {
        commit(themeID: previewThemeID)
    }

    func resetTheme() {
        commit(themeID: FunputConfiguration.defaultThemeID)
    }

    func reload() {
        configuration = store.load()
        appliedThemeID = Self.validThemeID(configuration.selectedThemeID)
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

    private func commit(themeID: String) {
        var candidate = configuration
        candidate.selectedThemeID = Self.validThemeID(themeID)
        guard store.save(candidate) else {
            showsSaveError = true
            return
        }
        configuration = candidate
        appliedThemeID = candidate.selectedThemeID
        previewThemeID = candidate.selectedThemeID
    }

    private static func validThemeID(_ id: String) -> String {
        BundledThemes.theme(id: id)?.id ?? FunputConfiguration.defaultThemeID
    }
}

enum AppearancePreviewMode: String, CaseIterable, Identifiable {
    case light
    case dark

    var id: String { rawValue }
    var title: String { self == .light ? "Sáng" : "Tối" }
    var interfaceStyle: UIUserInterfaceStyle { self == .light ? .light : .dark }
}
