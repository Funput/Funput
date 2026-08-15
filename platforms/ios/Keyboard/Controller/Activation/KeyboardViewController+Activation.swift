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
    let repairSnapshot: KeyboardBootstrapSnapshot?
}

/// What a presentation is built from, so an unchanged activation can skip the rebuild.
struct KeyboardActivationIdentity: Equatable {
    let configuration: FunputConfiguration
    let selectedTheme: KeyboardTheme
}

extension KeyboardViewController {
    func activateKeyboardForAppearance() {
        cancelClipboardRetry()
        let generation = activationState.begin()
        let source = loadActivationSource(hasFullAccess: hasFullAccess)

        launchTrace.measure("EngineConfiguration") {
            inputCoordinator.apply(source.configuration)
            applyTextInputTraits(force: true)
            _ = inputCoordinator.synchronizeDocument(
                makeDocumentWriter(),
                event: .activated
            )
        }
        adopt(source)
        // A panel left open in the previous host is not where the next one should
        // start; without this the keyboard comes back on emoji instead of letters.
        showFunput()
        activatePreferredHeightForAppearance()
        configurePersonalSuggestions(
            hasFullAccess: source.hasFullAccess,
            activationGeneration: generation
        )
    }

    /// Installs one activation's configuration, theme and presentation.
    ///
    /// Rebuilding the themed presentation means resolving the theme and re-reading the
    /// accessibility state, so an activation that changed nothing only refreshes the
    /// input-driven half of the presentation.
    func adopt(_ source: KeyboardActivationSource) {
        pendingBootstrapRepair = source.repairSnapshot
        let identity = KeyboardActivationIdentity(
            configuration: source.configuration,
            selectedTheme: source.selectedTheme
        )
        if adoptedIdentity != identity {
            adoptedIdentity = identity
            configuration = source.configuration
            themeCatalog = source.catalog
            selectedTheme = source.selectedTheme
            cachedThemedPresentation = KeyboardPresentationFactory.make(
                from: source.configuration,
                catalog: source.catalog
            )
            clipboardStore = ClipboardStore(expiry: source.configuration.clipboardExpiry)
        }
        launchTrace.measure("SurfaceApply") {
            applyInputPresentation()
            keyboardView.updateClipboardKeyVisible(configuration.clipboardEnabled)
            // A theme swap changes the backdrop without changing the layout, which on
            // its own would not schedule the layout pass that picks the new one up.
            // Clearing first also gives a failed decode one retry per activation.
            resolvedBackgroundRequest = nil
            refreshBackgroundImage()
        }
    }
}
