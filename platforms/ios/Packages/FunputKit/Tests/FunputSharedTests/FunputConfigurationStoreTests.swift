import Foundation
import Testing
import FunputShared

struct FunputConfigurationStoreTests {
    @Test("Loading from empty storage returns the default")
    func loadEmptyReturnsDefault() {
        withVolatileStore { store, _ in
            #expect(store.load() == .default)
            #expect(store.load().toneStyle == .modern)
        }
    }

    @Test("Saved configuration round-trips through storage")
    func saveThenLoad() {
        withVolatileStore { store, _ in
            var config = FunputConfiguration.default
            config.inputMethod = .telex
            config.selectedThemeID = "app.funput.theme.midnight"
            #expect(store.save(config))
            #expect(store.load() == config)
        }
    }

    @Test("Corrupt stored data falls back to the default")
    func corruptReturnsDefault() {
        withVolatileStore { store, defaults in
            defaults.set(Data([0x00, 0x01, 0x02]), forKey: FunputAppGroup.configurationKey)
            #expect(store.load() == .default)
        }
    }

    /// Runs `body` against a store backed by a throwaway defaults suite that is
    /// torn down afterward, so tests never touch shared preferences.
    private func withVolatileStore(_ body: (FunputConfigurationStore, UserDefaults) -> Void) {
        let suiteName = "test.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }
        body(FunputConfigurationStore(defaults: defaults), defaults)
    }
}

#if DEBUG
struct FunputUITestConfigurationOverrideStoreTests {
    @Test("UI-test override is separate, expiring, and clearable")
    func lifecycle() {
        let suiteName = "test.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defer { defaults.removePersistentDomain(forName: suiteName) }

        let realStore = FunputConfigurationStore(defaults: defaults)
        var real = FunputConfiguration.default
        real.inputMethod = .telex
        #expect(realStore.save(real))

        let overrideStore = FunputUITestConfigurationOverrideStore(defaults: defaults)
        var override = FunputConfiguration.default
        override.eagerRestore = false
        let now = Date(timeIntervalSince1970: 1_000)
        #expect(overrideStore.save(override, expiresAt: now.addingTimeInterval(60)))
        #expect(overrideStore.load(now: now) == override)
        #expect(realStore.load() == real)

        #expect(overrideStore.load(now: now.addingTimeInterval(61)) == nil)
        #expect(realStore.load() == real)

        #expect(overrideStore.save(override, expiresAt: now.addingTimeInterval(60)))
        overrideStore.clear()
        #expect(overrideStore.load(now: now) == nil)
    }
}
#endif
