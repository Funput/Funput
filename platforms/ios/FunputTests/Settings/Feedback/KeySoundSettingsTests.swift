import FunputShared
import Testing
@testable import Funput

@MainActor
struct KeySoundSettingsTests {
    @Test("A style is saved once and turns sound on; off keeps the style")
    func selectionAndOff() {
        let store = SettingsTestStore(configuration: .default)
        let bootstrap = KeyboardBootstrapTestStore()
        let model = SettingsModel(
            store: store, bootstrap: bootstrap,
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: true)
        )

        #expect(model.selectKeySoundStyle(.wood))
        #expect(model.selectedKeySoundStyle == .wood)
        #expect(store.configuration.isKeySoundEnabled)
        #expect(bootstrap.saves.last?.0.keySoundStyle == .wood)
        #expect(store.saveCount == 1)

        #expect(model.selectKeySoundStyle(.wood))
        #expect(store.saveCount == 1)

        #expect(model.selectKeySoundStyle(nil))
        #expect(model.selectedKeySoundStyle == nil)
        #expect(store.configuration.keySoundStyle == .wood)
    }

    @Test("Missing Full Access keeps the style pending until confirmation")
    func accessConfirmation() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(
            store: store,
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: false)
        )

        #expect(!model.selectKeySoundStyle(.bubble))
        #expect(model.selectedKeySoundStyle == nil)
        #expect(store.saveCount == 0)

        #expect(model.enableFullAccessFeature(.keySoundStyle(.bubble)))
        #expect(store.configuration.isKeySoundEnabled)
        #expect(store.configuration.keySoundStyle == .bubble)
        #expect(model.selectKeySoundStyle(nil))
    }

    @Test("The quick-settings switch turns the remembered style back on")
    func quickSwitchKeepsStyle() {
        var configuration = FunputConfiguration.default
        configuration.keySoundStyle = .mechanical
        let store = SettingsTestStore(configuration: configuration)
        let model = SettingsModel(
            store: store,
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: false)
        )

        #expect(model.enableFullAccessFeature(.sound))
        #expect(model.selectedKeySoundStyle == .mechanical)
    }

    @Test("Volume saves without Full Access and only when it changes")
    func volume() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(
            store: store,
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: false)
        )

        #expect(model.selectKeySoundVolume(.loud))
        #expect(store.configuration.keySoundVolume == .loud)
        #expect(model.selectKeySoundVolume(.loud))
        #expect(store.saveCount == 1)
    }

    @Test("A failed save keeps the old choice")
    func failedSave() {
        let store = SettingsTestStore(configuration: .default, acceptsSaves: false)
        let model = SettingsModel(
            store: store,
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: true)
        )

        #expect(!model.selectKeySoundStyle(.wood))
        #expect(!model.selectKeySoundVolume(.soft))
        #expect(model.showsSaveError)
        #expect(store.configuration == .default)
    }
}
