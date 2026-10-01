import FunputShared
import SwiftUI
import Testing
@testable import Funput

@MainActor
struct ExtraOnsetsSettingsTests {
    @Test("Master switch persists all letters and clears them on disabling")
    func masterSwitch() {
        let store = SettingsTestStore(configuration: .default)
        let bootstrap = KeyboardBootstrapTestStore()
        let model = SettingsModel(store: store, bootstrap: bootstrap)
        #expect(!model.extraOnsetsEnabledBinding.wrappedValue)
        model.extraOnsetsEnabledBinding.wrappedValue = true
        #expect(model.configuration.extraOnsets == .all)
        #expect(store.configuration.extraOnsets == .all)
        #expect(bootstrap.saves.last?.0.extraOnsets == .all)
        model.extraOnsetsEnabledBinding.wrappedValue = false
        #expect(model.configuration.extraOnsets.isEmpty)
        #expect(store.configuration.extraOnsets.isEmpty)
    }

    @Test("Each letter binding edits only its own selection")
    func individualLetters() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(store: store)
        for letter in ExtraOnsetLetters.letters {
            let binding = model.extraOnsetBinding(letter.member)
            #expect(!binding.wrappedValue)
            binding.wrappedValue = true
            #expect(model.configuration.extraOnsets == letter.member)
            #expect(model.extraOnsetsEnabledBinding.wrappedValue)
            binding.wrappedValue = false
            #expect(!model.extraOnsetsEnabledBinding.wrappedValue)
        }
        model.extraOnsetBinding(.z).wrappedValue = true
        model.extraOnsetBinding(.j).wrappedValue = true
        model.extraOnsetBinding(.z).wrappedValue = false
        #expect(model.configuration.extraOnsets == [.j])
        #expect(store.configuration.extraOnsets == [.j])
    }

    @Test("Stored selections survive reload and reset returns to off")
    func reloadAndReset() {
        let store = SettingsTestStore(configuration: FunputConfiguration(extraOnsets: [.z]))
        let model = SettingsModel(store: store)
        #expect(model.extraOnsetBinding(.z).wrappedValue)
        store.configuration.extraOnsets = [.f, .j]
        model.reload()
        #expect(model.configuration.extraOnsets == [.f, .j])
        model.reset()
        #expect(!model.extraOnsetsEnabledBinding.wrappedValue)
        #expect(store.configuration.extraOnsets.isEmpty)
    }

    @Test("Failed saves retain the previous letters and report the error")
    func failedSave() {
        let configuration = FunputConfiguration(extraOnsets: [.z])
        let store = SettingsTestStore(configuration: configuration, acceptsSaves: false)
        let model = SettingsModel(store: store)
        model.extraOnsetBinding(.f).wrappedValue = true
        #expect(model.configuration == configuration)
        #expect(store.configuration == configuration)
        #expect(model.showsSaveError)
    }

    @Test("Failed bootstrap rolls the selection back in storage and the model")
    func failedBootstrap() {
        let configuration = FunputConfiguration(extraOnsets: [.z])
        let store = SettingsTestStore(configuration: configuration)
        let model = SettingsModel(store: store, bootstrap: KeyboardBootstrapTestStore(acceptsSaves: false))
        model.extraOnsetsEnabledBinding.wrappedValue = false
        #expect(model.configuration == configuration)
        #expect(store.configuration == configuration)
        #expect(model.extraOnsetsEnabledBinding.wrappedValue)
        #expect(model.showsSaveError)
    }
}
