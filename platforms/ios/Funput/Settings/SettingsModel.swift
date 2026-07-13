import FunputShared
import KeyboardLayout
import Observation
import SwiftUI

@MainActor @Observable
final class SettingsModel {
    private(set) var configuration: FunputConfiguration
    var showsSaveError = false
    private let store: any FunputConfigurationStoring

    init(store: any FunputConfigurationStoring) {
        self.store = store
        configuration = store.load()
    }

    var inputMethodLabel: String { configuration.inputMethod.settingsTitle }
    var languageLabel: String { configuration.language.displayLabel }
    var toneStyleLabel: String { configuration.toneStyle.settingsTitle }

    func reload() {
        configuration = store.load()
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

    var heightBinding: Binding<Double> {
        Binding(
            get: { self.configuration.heightScale },
            set: { self.update(\.heightScale, to: min(max($0, 0.85), 1.15)) }
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
