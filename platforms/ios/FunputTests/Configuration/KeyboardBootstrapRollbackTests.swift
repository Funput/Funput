import Foundation
import FunputShared
import KeyboardConfiguration
import Testing
import ThemeRuntime
import ThemeSchema
@testable import Funput

@MainActor
struct KeyboardBootstrapRollbackTests {
    @Test("Settings rollback domain configuration when snapshot save fails")
    func settingsRollback() {
        let configuration = SettingsTestStore(configuration: .default)
        let model = SettingsModel(
            store: configuration,
            bootstrap: KeyboardBootstrapTestStore(acceptsSaves: false)
        )

        model.update(\.smartRestore, to: false)

        #expect(configuration.configuration == .default)
        #expect(model.configuration == .default)
        #expect(model.showsSaveError)
    }

    @Test("Applying a theme rolls configuration back on snapshot failure")
    func applyRollback() {
        let configuration = SettingsTestStore(configuration: .default)
        let model = AppearanceModel(
            store: configuration,
            customStore: ThemeTestStore(),
            bootstrap: KeyboardBootstrapTestStore(acceptsSaves: false)
        )
        model.selectTheme(KeyboardTheme.midnight.id)

        model.applyPreview()

        #expect(configuration.configuration == .default)
        #expect(model.appliedThemeID == FunputConfiguration.defaultThemeID)
        #expect(model.showsSaveError)
    }

    @Test("Theme and new asset rollback when snapshot save fails")
    func themeAndAssetRollback() {
        let themes = ThemeTestStore()
        let assets = PreviewThemeAssetStore()
        let model = AppearanceModel(
            store: SettingsTestStore(configuration: .default),
            customStore: themes,
            assetStore: assets,
            bootstrap: KeyboardBootstrapTestStore(acceptsSaves: false)
        )
        var draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))
        draft.installImage(ProcessedThemeImage(source: Data([1]), rendered: Data([2])))

        #expect(!model.save(draft))
        #expect(themes.load().isEmpty)
        #expect(assets.assetCount == 0)
    }

    @Test("Theme deletion rolls every domain back on snapshot failure")
    func deleteRollback() {
        let bootstrap = KeyboardBootstrapTestStore()
        let themes = ThemeTestStore()
        let configuration = SettingsTestStore(configuration: .default)
        let model = AppearanceModel(
            store: configuration,
            customStore: themes,
            bootstrap: bootstrap
        )
        let draft = model.makeDraft(for: .create(baseThemeID: KeyboardTheme.midnight.id))
        #expect(model.save(draft))
        model.applyPreview()
        bootstrap.acceptsSaves = false

        #expect(!model.deletePreviewCustomTheme())
        #expect(themes.load().map(\.id) == [draft.customTheme.id])
        #expect(configuration.configuration.selectedThemeID == draft.customTheme.id)
    }

    @Test("First-run seed writes a loadable snapshot")
    func firstRunSeed() throws {
        let container = FileManager.default.temporaryDirectory
            .appendingPathComponent("BootstrapSeed-\(UUID().uuidString)")
        defer { try? FileManager.default.removeItem(at: container) }
        let store = KeyboardBootstrapSnapshotStore(containerURL: container)
        let synchronizer = KeyboardBootstrapSynchronizer(store: store)

        #expect(synchronizer.save(configuration: .default, customThemes: []))
        #expect(try store.load().selectedTheme == BundledThemes.default)
    }
}
