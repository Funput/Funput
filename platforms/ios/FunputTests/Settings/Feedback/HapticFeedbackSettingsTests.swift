import FunputShared
import Testing
@testable import Funput

@MainActor
struct HapticFeedbackSettingsTests {
    @Test("A strength is saved once; selecting it again only requests a preview")
    func selectionAndReselection() {
        let store = SettingsTestStore(configuration: .default)
        let bootstrap = KeyboardBootstrapTestStore()
        let model = SettingsModel(
            store: store, bootstrap: bootstrap,
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: true)
        )

        #expect(model.selectHapticStrength(.strong))
        #expect(model.selectedHapticStrength == .strong)
        #expect(store.configuration.hapticFeedbackStrength == .strong)
        #expect(bootstrap.saves.last?.0.hapticFeedbackStrength == .strong)
        #expect(store.saveCount == 1)

        #expect(model.selectHapticStrength(.strong))
        #expect(store.saveCount == 1)
        #expect(bootstrap.saves.count == 1)

        #expect(model.selectHapticStrength(nil))
        #expect(model.selectedHapticStrength == nil)
        #expect(store.configuration.hapticFeedbackStrength == .strong)
    }

    @Test("Missing Full Access keeps the choice pending until confirmation")
    func accessConfirmationPreservesStrength() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(
            store: store,
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: false)
        )

        #expect(!model.selectHapticStrength(.medium))
        #expect(model.selectedHapticStrength == nil)
        #expect(store.saveCount == 0)

        #expect(model.enableFullAccessFeature(.hapticStrength(.medium)))
        #expect(store.configuration.isHapticFeedbackEnabled)
        #expect(store.configuration.hapticFeedbackStrength == .medium)
        #expect(model.selectHapticStrength(nil))
        #expect(store.configuration.hapticFeedbackStrength == .medium)
    }

    @Test("Failed saves preserve the old choice and do not request a preview")
    func failedSave() {
        let store = SettingsTestStore(configuration: .default, acceptsSaves: false)
        let model = SettingsModel(
            store: store,
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: true)
        )

        #expect(!model.selectHapticStrength(.strong))
        #expect(model.selectedHapticStrength == nil)
        #expect(model.showsSaveError)
        #expect(!model.enableFullAccessFeature(.hapticStrength(.medium)))
        #expect(store.configuration == .default)
    }

    @Test("A failed bootstrap rolls the strength and enabled flag back together")
    func failedBootstrap() {
        let store = SettingsTestStore(configuration: .default)
        let model = SettingsModel(
            store: store, bootstrap: KeyboardBootstrapTestStore(acceptsSaves: false),
            accessStore: KeyboardAccessStateTestStore(hasObservedFullAccess: true)
        )

        #expect(!model.selectHapticStrength(.strong))
        #expect(model.configuration == .default)
        #expect(store.configuration == .default)
        #expect(model.showsSaveError)
    }
}
