import FunputShared
import KeyboardInput
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

extension KeyboardViewController {
    /// Reloads shared configuration and applies it to the engine and surface.
    ///
    /// Called on load and on every activation so preference changes made in the
    /// containing app take effect the next time the keyboard appears. Reading on
    /// activation (not per keystroke) keeps the hot path free of I/O.
    func reloadConfiguration() {
#if DEBUG
        configuration = FunputUITestConfigurationOverrideStore().load()
            ?? configurationStore.load()
#else
        configuration = configurationStore.load()
#endif
        themeCatalog = ThemeCatalog(customThemes: customThemeStore.load())
        let theme = themeCatalog.theme(id: configuration.selectedThemeID)
        let assetID = theme?.backgroundEffects.image?.assetID
        let data = assetID.flatMap(themeAssetStore.renderedData)
        let image = data.flatMap(UIImage.init(data:))
        keyboardView.backgroundImage = image
        emojiView.backgroundImage = image
        inputCoordinator.apply(configuration)
        updateInputPresentation()
    }
}
