import Foundation
import Testing
import ThemeRuntime
import ThemeSchema

struct ThemeCatalogStoreTests {
    @Test("Catalog resolves bundled, custom, and missing identifiers")
    func catalogLookup() {
        let custom = fixture()
        let catalog = ThemeCatalog(customThemes: [custom])

        #expect(catalog.theme(id: KeyboardTheme.midnight.id) == .midnight)
        #expect(catalog.theme(id: custom.id) == custom.theme)
        #expect(catalog.theme(id: "missing") == nil)
        #expect(catalog.baseTheme(for: custom) == .midnight)
    }

    @Test("Store creates, updates, and deletes themes")
    func mutations() {
        let (store, defaults, suite) = makeStore()
        defer { defaults.removePersistentDomain(forName: suite) }
        var custom = fixture()

        #expect(store.upsert(custom))
        #expect(store.load() == [custom])
        custom.theme.metadata.name = "Updated"
        #expect(store.upsert(custom))
        #expect(store.load() == [custom])
        #expect(store.delete(id: custom.id))
        #expect(store.load().isEmpty)
    }

    @Test("Corrupt persisted data falls back to an empty catalog")
    func corruptData() {
        let (store, defaults, suite) = makeStore()
        defer { defaults.removePersistentDomain(forName: suite) }
        defaults.set(Data("not-json".utf8), forKey: "themes")

        #expect(store.load().isEmpty)
    }

    private func fixture() -> CustomKeyboardTheme {
        CustomKeyboardTheme(
            baseTheme: .midnight,
            uuid: UUID(uuidString: "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE")!
        )
    }

    private func makeStore() -> (CustomThemeStore, UserDefaults, String) {
        let suite = "ThemeCatalogStoreTests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suite)!
        return (CustomThemeStore(defaults: defaults, key: "themes"), defaults, suite)
    }
}
