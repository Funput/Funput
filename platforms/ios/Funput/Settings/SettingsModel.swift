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

    init(
        store: any FunputConfigurationStoring,
        accessStore: any KeyboardAccessStateReading = KeyboardAccessStateStore()
    ) {
        self.store = store
        self.accessStore = accessStore
        configuration = store.load()
        hasFullAccess = accessStore.hasObservedFullAccess
    }

    var inputMethodLabel: String { configuration.inputMethod.settingsTitle }
    var languageLabel: String { configuration.language.displayLabel }
    var toneStyleLabel: String { configuration.toneStyle.settingsTitle }
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
        commit(.default)
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
    var settingsTitle: String { self == .vni ? "VNI" : "Telex" }
}

extension ToneStyleOption {
    var settingsTitle: String { self == .traditional ? "Truyền thống" : "Hiện đại" }
}
