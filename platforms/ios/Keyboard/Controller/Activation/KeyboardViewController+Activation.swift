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
        // Ahead of the engine block: resolving the field's traits reads the
        // "Tự viết hoa" preference, and the document sync that follows turns those
        // traits into the initial Shift state.
        adoptConfiguration(source)

        launchTrace.measure("EngineConfiguration") {
            inputCoordinator.apply(source.configuration)
            applyTextInputTraits(force: true)
            _ = inputCoordinator.synchronizeDocument(
                makeDocumentWriter(),
                event: .activated
            )
        }
        applyAdoptedPresentation()
        // A panel left open in the previous host is not where the next one should
        // start; without this the keyboard comes back on emoji instead of letters.
        showFunput()
        activatePreferredHeightForAppearance()
        configurePersonalSuggestions(
            hasFullAccess: source.hasFullAccess,
            activationGeneration: generation
        )
    }

    /// Installs one activation's configuration and theme.
    ///
    /// Rebuilding the themed presentation means resolving the theme and re-reading the
    /// accessibility state, so an activation that changed nothing keeps the last one.
    func adoptConfiguration(_ source: KeyboardActivationSource) {
        pendingBootstrapRepair = source.repairSnapshot
        let identity = KeyboardActivationIdentity(
            configuration: source.configuration,
            selectedTheme: source.selectedTheme
        )
        guard adoptedIdentity != identity else { return }
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

    /// Folds the adopted configuration and the live input state onto the surfaces.
    func applyAdoptedPresentation() {
        launchTrace.measure("SurfaceApply") {
            applyKeyboardAppearance()
            applyInputPresentation()
            keyboardView.updateClipboardKeyVisible(configuration.clipboardEnabled)
            // A theme swap changes the backdrop without changing the layout, which on
            // its own would not schedule the layout pass that picks the new one up.
            // Clearing first also gives a failed decode one retry per activation.
            resolvedBackgroundRequest = nil
            refreshBackgroundImage()
        }
    }

    /// Pins the whole surface to the user's chosen appearance.
    ///
    /// Every color the renderer resolves reads the view's own trait collection, and so
    /// does the Liquid Glass material — glass adapts from the trait environment, not from
    /// the theme. Carrying a resolved style through `KeyboardPresentation` instead would
    /// flip the tints while leaving the glass on the host app's appearance.
    ///
    /// This runs on every activation rather than once in `viewDidLoad` because the
    /// setting can change between two keyboard sessions; the guard keeps an unchanged
    /// appearance from spending a trait-change pass.
    private func applyKeyboardAppearance() {
        let style = configuration.keyboardAppearance.interfaceStyle
        guard view.overrideUserInterfaceStyle != style else { return }
        view.overrideUserInterfaceStyle = style
    }
}
