import FunputShared
import KeyboardConfiguration
import KeyboardInput
import KeyboardRenderer
import ThemeRuntime
import ThemeSchema
import UIKit

struct KeyboardActivationSource {
    let configuration: FunputConfiguration
    let catalog: ThemeCatalog
    let selectedTheme: KeyboardTheme
    let hasFullAccess: Bool
}

private struct KeyboardActivationPayload {
    let source: KeyboardActivationSource
    let themedPresentation: KeyboardPresentation
    let presentation: KeyboardPresentation
    let backgroundImage: UIImage?
    let generation: UInt64
}

extension KeyboardViewController {
    func activateKeyboardForAppearance() {
        cancelClipboardRetry()
        let generation = activationState.begin()
        let source = loadActivationSource(hasFullAccess: hasFullAccess)
        if source.hasFullAccess { accessStateStore.recordFullAccess() }

        launchTrace.measure("EngineConfiguration") {
            inputCoordinator.apply(source.configuration)
            applyTextInputTraits(force: true)
            _ = inputCoordinator.synchronizeDocument(
                makeDocumentWriter(),
                event: .activated
            )
        }
        let themed = KeyboardPresentationFactory.make(
            from: source.configuration,
            catalog: source.catalog
        )
        let presentation = makeInputPresentation(
            configuration: source.configuration,
            themed: themed
        )
        let image = loadBackgroundImage(
            assetID: source.selectedTheme.backgroundEffects.image?.assetID
        )
        commitActivation(
            KeyboardActivationPayload(
                source: source,
                themedPresentation: themed,
                presentation: presentation,
                backgroundImage: image,
                generation: generation
            )
        )
        activatePreferredHeightForAppearance()
    }

    private func commitActivation(_ payload: KeyboardActivationPayload) {
        configuration = payload.source.configuration
        themeCatalog = payload.source.catalog
        selectedTheme = payload.source.selectedTheme
        cachedThemedPresentation = payload.themedPresentation
        currentPresentation = payload.presentation
        clipboardStore = ClipboardStore(expiry: configuration.clipboardExpiry)
        launchTrace.measure("SurfaceApply") {
            keyboardView.updateClipboardKeyVisible(configuration.clipboardEnabled)
            applyPresentationToSurfaces(payload.presentation)
            applyBackgroundImage(payload.backgroundImage)
        }
        configurePersonalSuggestions(
            hasFullAccess: payload.source.hasFullAccess,
            activationGeneration: payload.generation
        )
    }
}
