import Foundation
@testable import FunputShared
import Testing

@Suite("Keyboard access state store")
struct KeyboardAccessStateStoreTests {
    private func withStore(
        notify: @escaping () -> Void = {},
        body: (KeyboardAccessStateStore) -> Void
    ) {
        let suite = "KeyboardAccessStateStoreTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite) ?? .standard
        defer { defaults.removePersistentDomain(forName: suite) }
        body(KeyboardAccessStateStore(defaults: defaults, notify: notify))
    }

    @Test("Recording Full Access latches true and notifies once")
    func latchesAndNotifiesOnce() {
        var notifications = 0
        withStore(notify: { notifications += 1 }) { store in
            #expect(!store.hasObservedFullAccess)
            store.recordFullAccess()
            #expect(store.hasObservedFullAccess)
            #expect(notifications == 1)

            store.recordFullAccess()
            #expect(store.hasObservedFullAccess)
            #expect(notifications == 1)
        }
    }

    @Test("An already latched store does not notify again")
    func alreadyLatchedIsQuiet() {
        var notifications = 0
        let suite = "KeyboardAccessStateStoreTests.latched.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite) ?? .standard
        defer { defaults.removePersistentDomain(forName: suite) }
        defaults.set(true, forKey: FunputAppGroup.observedFullAccessKey)

        let store = KeyboardAccessStateStore(defaults: defaults, notify: { notifications += 1 })
        store.recordFullAccess()
        #expect(store.hasObservedFullAccess)
        #expect(notifications == 0)
    }
}
