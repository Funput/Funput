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

/// What `viewDidLoad` read, kept for the presentation that follows it.
///
/// That presentation's `viewWillAppear` lands inside the system's animation, so
/// reading the configuration a second time there would sit on the critical path for
/// nothing: no one can change the settings in between (bootstrap to `viewDidAppear`
/// measured about 140ms on a cold start). The freshness bound covers a controller that iOS loads and only
/// presents much later, by which time the settings may have moved.
struct KeyboardBootstrappedSource {
    static let freshness: Duration = .seconds(1)

    let source: KeyboardActivationSource
    let loadedAt: ContinuousClock.Instant

    var isFresh: Bool { loadedAt.duration(to: .now) < Self.freshness }
}

/// What a presentation is built from, so an unchanged activation can skip the rebuild.
struct KeyboardActivationIdentity: Equatable {
    let configuration: FunputConfiguration
    let selectedTheme: KeyboardTheme
    /// The resolved theme depends on it, and the identity outlives a host app.
    let reducesTransparency: Bool
}

extension KeyboardViewController {
    func activateKeyboardForAppearance() {
        cancelClipboardRetry()
        let generation = activationState.begin()
        let source = takeBootstrappedSource() ?? loadActivationSource(hasFullAccess: hasFullAccess)
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
        activateShortcuts()
        applyAdoptedPresentation(claimingAt: .appearing)
        // A panel left open in the previous host is not where the next one should
        // start; without this the keyboard comes back on emoji instead of letters.
        showFunput()
        activatePreferredHeightForAppearance()
        configurePersonalSuggestions(
            hasFullAccess: source.hasFullAccess,
            activationGeneration: generation
        )
    }

    /// The first appearance after `viewDidLoad` takes what bootstrap read, once and
    /// only while fresh; every later appearance reads afresh, since the user may have
    /// changed the settings while the keyboard was away.
    private func takeBootstrappedSource() -> KeyboardActivationSource? {
        defer { bootstrappedSource = nil }
        guard let bootstrappedSource, bootstrappedSource.isFresh else { return nil }
        return bootstrappedSource.source
    }

    /// The configuration and theme are the process's; only what to repair in the
    /// bootstrap snapshot belongs to this activation.
    func adoptConfiguration(_ source: KeyboardActivationSource) {
        pendingBootstrapRepair = source.repairSnapshot
        runtime.adopt(source)
    }

    /// Folds the adopted configuration and the live input state onto the surfaces,
    /// asking for the primary surface on the way.
    ///
    /// The presentation is worked out first, so a surface built by this claim is
    /// built for it, and a surface taken over from another host app is shown this
    /// controller's keyboard in one pass. A controller that does not get the surface
    /// stops there: it has its layout and its height, which is all it needs until it
    /// appears.
    func applyAdoptedPresentation(claimingAt moment: KeyboardSurfaceClaimPolicy.Moment) {
        launchTrace.measure("SurfaceApply") {
            applyKeyboardAppearance()
            applyInputPresentation()
            claimSurface(at: moment)
            guard let surface = ownedSurface else { return }
            surface.updateClipboardKeyVisible(configuration.clipboardEnabled)
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
