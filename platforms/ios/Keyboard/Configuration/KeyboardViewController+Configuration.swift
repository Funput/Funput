import FunputShared
import KeyboardConfiguration
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

extension KeyboardViewController {
    func loadActivationSource(hasFullAccess: Bool) -> KeyboardActivationSource {
        launchTrace.beginConfigurationLoad()
        defer { launchTrace.endConfigurationLoad() }
#if DEBUG
        if let override = FunputUITestConfigurationOverrideStore().load() {
            return makeFallbackSource(
                configuration: override,
                hasFullAccess: hasFullAccess,
                repairsSnapshot: false
            )
        }
#endif
        if let snapshot = try? bootstrapSnapshotStore.load() {
            launchTrace.recordBootstrapSnapshot(hit: true)
            return makeSnapshotSource(snapshot, hasFullAccess: hasFullAccess)
        }
        launchTrace.recordBootstrapSnapshot(hit: false)
        return makeFallbackSource(
            configuration: configurationStore.load(),
            hasFullAccess: hasFullAccess,
            repairsSnapshot: true
        )
    }

    func repairBootstrapSnapshotIfNeeded() {
        guard let snapshot = pendingBootstrapRepair else { return }
        pendingBootstrapRepair = nil
        let store = bootstrapSnapshotStore
        Task.detached(priority: .utility) {
            try? store.repairIfNeeded(snapshot)
        }
    }

    private func makeFallbackSource(
        configuration: FunputConfiguration,
        hasFullAccess: Bool,
        repairsSnapshot: Bool
    ) -> KeyboardActivationSource {
        launchTrace.recordCustomThemeCatalogLoad()
        let customThemes = customThemeStore.load()
        let resolved = launchTrace.measure("ThemeResolve") {
            KeyboardActivationThemeResolver.resolve(
                configuration: configuration,
                customThemes: customThemes
            )
        }
        return KeyboardActivationSource(
            configuration: configuration,
            catalog: resolved.catalog,
            selectedTheme: resolved.selectedTheme,
            hasFullAccess: hasFullAccess,
            repairSnapshot: repairsSnapshot ? KeyboardBootstrapSnapshot.make(
                configuration: configuration,
                customThemes: customThemes
            ) : nil
        )
    }

    private func makeSnapshotSource(
        _ snapshot: KeyboardBootstrapSnapshot,
        hasFullAccess: Bool
    ) -> KeyboardActivationSource {
        let bundled = BundledThemes.theme(id: snapshot.selectedTheme.id) != nil
        let customThemes = bundled ? [] : [CustomKeyboardTheme(
            baseThemeID: BundledThemes.default.id,
            theme: snapshot.selectedTheme
        )]
        return KeyboardActivationSource(
            configuration: snapshot.configuration,
            catalog: ThemeCatalog(customThemes: customThemes),
            selectedTheme: snapshot.selectedTheme,
            hasFullAccess: hasFullAccess,
            repairSnapshot: nil
        )
    }

    func applyBackgroundImage(_ image: UIImage?) {
        keyboardView.backgroundImage = image
        emojiView?.backgroundImage = image
        kaomojiView?.backgroundImage = image
        clipboardPanelView?.backgroundImage = image
    }

    func loadBackgroundImage(assetID: String?) -> UIImage? {
        backgroundImageCache.resolve(
            assetID: assetID,
            load: { id in
                launchTrace.measure("AssetRead") {
                    themeAssetStore.renderedData(for: id)
                }
            },
            decode: { data in
                launchTrace.measure("AssetDecode") { UIImage(data: data) }
            }
        )
    }
}
