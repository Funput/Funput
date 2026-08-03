import FunputShared
import KeyboardLayout
import Observation
import SwiftUI

@MainActor @Observable
final class SettingsModel {
    private(set) var configuration: FunputConfiguration
    private(set) var hasFullAccess: Bool
    var showsSaveError = false
    private let store: any FunputConfigurationStoring
    private let accessStore: any KeyboardAccessStateReading
    private let clipboardStore: ClipboardStore

    init(
        store: any FunputConfigurationStoring,
        accessStore: any KeyboardAccessStateReading = KeyboardAccessStateStore(),
        clipboardStore: ClipboardStore = ClipboardStore()
    ) {
        self.store = store
        self.accessStore = accessStore
        self.clipboardStore = clipboardStore
        configuration = store.load()
        hasFullAccess = accessStore.hasObservedFullAccess
    }

    var inputMethodLabel: String { configuration.inputMethod.settingsTitle }
    var languageLabel: String { configuration.language.displayLabel }
    var toneStyleLabel: String { configuration.toneStyle.settingsTitle }
    var clipboardExpiryLabel: String { configuration.clipboardExpiry.title }
    var isNumberRowLocked: Bool { configuration.inputMethod == .vni }

    func reload() {
        configuration = store.load()
        hasFullAccess = accessStore.hasObservedFullAccess
    }

    func update<Value>(_ keyPath: WritableKeyPath<FunputConfiguration, Value>, to value: Value) {
        var candidate = configuration
        candidate[keyPath: keyPath] = value
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

    func boolBinding(_ keyPath: WritableKeyPath<FunputConfiguration, Bool>) -> Binding<Bool> {
        Binding(
            get: { self.configuration[keyPath: keyPath] },
            set: { self.update(keyPath, to: $0) }
        )
    }

    var numberRowBinding: Binding<Bool> {
        Binding(
            get: { self.isNumberRowLocked || self.configuration.showsNumberRow },
            set: { enabled in
                guard !self.isNumberRowLocked else { return }
                self.update(\.showsNumberRow, to: enabled)
            }
        )
    }

    func fullAccessBinding(
        _ keyPath: WritableKeyPath<FunputConfiguration, Bool>,
        requestAccess: @escaping () -> Void
    ) -> Binding<Bool> {
        Binding(
            get: { self.configuration[keyPath: keyPath] },
            set: { enabled in
                if enabled && !self.hasFullAccess { requestAccess() }
                else { self.update(keyPath, to: enabled) }
            }
        )
    }

    var heightBinding: Binding<Double> {
        Binding(
            get: { self.configuration.heightScale },
            set: { self.update(\.heightScale, to: min(max($0, 0.85), 1.2)) }
        )
    }

    var saveErrorBinding: Binding<Bool> {
        Binding(get: { self.showsSaveError }, set: { self.showsSaveError = $0 })
    }

    private func commit(_ candidate: FunputConfiguration) {
        guard store.save(candidate) else {
            showsSaveError = true
            return
        }
        configuration = candidate
    }
}

extension KeyboardInputMethod {
    var settingsTitle: String {
        switch self {
        case .telex: "Telex"
        case .telexAdvanced: "Telex nâng cao"
        case .vni: "VNI"
        }
    }

    var settingsSummary: String {
        switch self {
        case .telex: "Dùng tổ hợp chữ để nhập dấu."
        case .telexAdvanced: "Full Telex — [→ư, ]→ơ, w đầu từ→ư."
        case .vni: "Dùng các phím số để nhập dấu."
        }
    }
}

extension ToneStyleOption {
    var settingsTitle: String { self == .traditional ? "Truyền thống" : "Hiện đại" }
}
