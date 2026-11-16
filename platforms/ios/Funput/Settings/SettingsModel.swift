import Combine
import FunputShared
import KeyboardLayout
import SwiftUI
import ThemeRuntime
import ThemeSchema

@MainActor
final class SettingsModel: ObservableObject {
    @Published private(set) var configuration: FunputConfiguration
    @Published private(set) var hasFullAccess: Bool
    @Published var showsSaveError = false
    private let store: any FunputConfigurationStoring
    private let customStore: any CustomThemeStoring
    private let bootstrap: any KeyboardBootstrapSynchronizing
    private let accessStore: any KeyboardAccessStateReading
    private let clipboardStore: ClipboardStore

    init(
        store: any FunputConfigurationStoring,
        customStore: any CustomThemeStoring = PreviewCustomThemeStore(),
        bootstrap: any KeyboardBootstrapSynchronizing = NoopKeyboardBootstrapSynchronizer(),
        accessStore: any KeyboardAccessStateReading = KeyboardAccessStateStore(),
        clipboardStore: ClipboardStore = ClipboardStore()
    ) {
        self.store = store
        self.customStore = customStore
        self.bootstrap = bootstrap
        self.accessStore = accessStore
        self.clipboardStore = clipboardStore
        configuration = store.load()
        hasFullAccess = accessStore.hasObservedFullAccess
    }

    var inputMethodLabel: String { configuration.inputMethod.settingsTitle }
    var layoutPresetLabel: String { configuration.layoutPreset.settingsTitle }
    var keySizingLabel: String { configuration.keySizing.settingsTitle }
    var languageLabel: String { configuration.language.displayLabel }
    var toneStyleLabel: String { configuration.toneStyle.settingsTitle }
    var clipboardExpiryLabel: String { configuration.clipboardExpiry.title }
    var isNumberRowLocked: Bool { configuration.inputMethod == .vni }

    func reload() {
        configuration = store.load()
        refreshFullAccess()
    }

    /// Re-reads only the keyboard's Full Access flag and publishes it only when it
    /// changed, so polling it does not redraw every screen. Returns whether it changed.
    @discardableResult
    func refreshFullAccess() -> Bool {
        let observed = accessStore.hasObservedFullAccess
        guard observed != hasFullAccess else { return false }
        hasFullAccess = observed
        return true
    }

    func update<Value>(_ keyPath: WritableKeyPath<FunputConfiguration, Value>, to value: Value) {
        var candidate = configuration
        candidate[keyPath: keyPath] = value
        commit(candidate)
    }

    /// Writes several fields as one change, so a setting that means two things in the
    /// stored shape still reaches the keyboard as a single save.
    func update(_ mutate: (inout FunputConfiguration) -> Void) {
        var candidate = configuration
        mutate(&candidate)
        commit(candidate)
    }

    func selectKeySizing(_ sizing: KeyboardKeySizing) {
        var candidate = configuration
        candidate.keySizing = sizing
        if sizing == .system {
            candidate.selectedThemeID = KeyboardTheme.iosSystem.id
        }
        commit(candidate)
    }

    func reset() {
        var defaults = FunputConfiguration.default
        defaults.personalSuggestionResetToken = configuration.personalSuggestionResetToken
        commit(defaults)
    }

    func requestPersonalSuggestionReset() {
        update(\.personalSuggestionResetToken, to: UUID())
    }

    /// Cleared straight away rather than through a token like the personal lexicon:
    /// that store belongs to Rust and the app cannot reach it, while the clipboard
    /// file sits in the App Group. For a privacy action, "deleted" should mean now —
    /// not at the next keyboard launch.
    func clearClipboardHistory() {
        clipboardStore.clear()
    }

    private func commit(_ candidate: FunputConfiguration) {
        let previous = configuration
        guard store.save(candidate) else {
            showsSaveError = true
            return
        }
        guard bootstrap.save(
            configuration: candidate,
            customThemes: customStore.load()
        ) else {
            _ = store.save(previous)
            showsSaveError = true
            return
        }
        configuration = candidate
    }
}
