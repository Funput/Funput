import FunputShared
import KeyboardInput
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

extension KeyboardViewController {
    /// Reads the shared configuration once per activation. Presentation and
    /// engine updates are deliberately coordinated by `viewWillAppear`.
    func reloadConfiguration() {
        launchTrace.beginConfigurationLoad()
        defer { launchTrace.endConfigurationLoad() }
#if DEBUG
        configuration = FunputUITestConfigurationOverrideStore().load()
            ?? configurationStore.load()
#else
        configuration = configurationStore.load()
#endif
        themeCatalog = ThemeCatalog(customThemes: customThemeStore.load())
        cachedThemedPresentation = nil
        selectedTheme = themeCatalog.theme(id: configuration.selectedThemeID)
            ?? BundledThemes.default
        updateCachedBackgroundImage(assetID: selectedTheme.backgroundEffects.image?.assetID)
    }

    func applyConfigurationForActivation() {
        clipboardStore = ClipboardStore(expiry: configuration.clipboardExpiry)
        keyboardView.updateClipboardKeyVisible(configuration.clipboardEnabled)
        inputCoordinator.apply(configuration)
        applyTextInputTraits(force: true)
        // Reconcile before rendering so activation cannot trigger a second
        // presentation pass for capitalization or secure-field state.
        _ = inputCoordinator.synchronizeDocument(
            makeDocumentWriter(),
            event: .activated
        )
        configurePersonalSuggestions()
    }

    func applyCachedBackgroundImage() {
        keyboardView.backgroundImage = cachedBackgroundImage
        emojiView?.backgroundImage = cachedBackgroundImage
        kaomojiView?.backgroundImage = cachedBackgroundImage
        clipboardPanelView?.backgroundImage = cachedBackgroundImage
    }

    private func updateCachedBackgroundImage(assetID: String?) {
        guard assetID != cachedBackgroundAssetID else { return }
        cachedBackgroundAssetID = assetID
        let data = assetID.flatMap(themeAssetStore.renderedData)
        cachedBackgroundImage = data.flatMap(UIImage.init(data:))
    }
}
