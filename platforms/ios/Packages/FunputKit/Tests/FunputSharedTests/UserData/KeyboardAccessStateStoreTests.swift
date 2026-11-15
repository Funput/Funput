import Foundation
@testable import FunputShared
import Testing

@Suite("Keyboard access state store")
struct KeyboardAccessStateStoreTests {
    @Test("Recording a new state stores it and signals once")
    func recordsAndSignalsChange() {
        withStore { store, notifications in
            #expect(!store.hasObservedFullAccess)
            store.record(hasFullAccess: true)
            #expect(store.hasObservedFullAccess)
            #expect(notifications() == 1)

            store.record(hasFullAccess: true)
            #expect(notifications() == 1)
        }
    }

    @Test("Revoked Full Access is recorded too")
    func recordsRevocation() {
        withStore(initial: true) { store, notifications in
            store.record(hasFullAccess: false)
            #expect(!store.hasObservedFullAccess)
            #expect(notifications() == 1)
        }
    }

    @Test("Recording the stored state is quiet")
    func unchangedStateIsQuiet() {
        withStore { store, notifications in
            store.record(hasFullAccess: false)
            #expect(!store.hasObservedFullAccess)
            #expect(notifications() == 0)
        }
    }

    private func withStore(
        initial: Bool? = nil,
        body: (KeyboardAccessStateStore, () -> Int) -> Void
    ) {
        let suite = "KeyboardAccessStateStoreTests.\(UUID().uuidString)"
        guard let defaults = UserDefaults(suiteName: suite) else {
            Issue.record("Could not create a defaults suite")
            return
        }
        defer { defaults.removePersistentDomain(forName: suite) }
        if let initial { defaults.set(initial, forKey: FunputAppGroup.observedFullAccessKey) }

        var notifications = 0
        let store = KeyboardAccessStateStore(defaults: defaults, notify: { notifications += 1 })
        body(store, { notifications })
    }
}
