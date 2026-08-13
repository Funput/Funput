import FunputShared
import KeyboardConfiguration
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

extension KeyboardViewController {
    func loadActivationSource(hasFullAccess: Bool) -> KeyboardActivationSource {
        launchTrace.beginConfigurationLoad()
#if DEBUG
        let configuration = FunputUITestConfigurationOverrideStore().load()
            ?? configurationStore.load()
#else
        let configuration = configurationStore.load()
#endif
        let customThemes = customThemeStore.load()
        launchTrace.endConfigurationLoad()
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
            hasFullAccess: hasFullAccess
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
