import KeyboardInput
import KeyboardRenderer
import PersonalSuggestions
import UIKit

extension KeyboardViewController: KeyboardSurfaceClaimant {
    var surfaceContainer: UIView { view }

    var isSurfaceOwner: Bool { runtime.surfaceHost.isHeld(by: self) }

    /// The process's surface, for the controller that holds it and nobody else.
    ///
    /// Everything a controller does to the surface goes through this, so a controller
    /// that has lost it, or has not yet been given it, writes nowhere. iOS keeps
    /// calling a controller after the next one is on screen: without this each of
    /// those calls would land on the keyboard the user is now typing on.
    var ownedSurface: KeyboardSurfaceView? {
        isSurfaceOwner ? runtime.surfaceHost.surface : nil
    }

    /// The process's suggestion service, under the same rule as the surface: a
    /// controller that does not hold the keyboard must not feed it a prefix, or one
    /// app's word would be completed on another app's bar.
    var ownedSuggestionService: PersonalSuggestionService? {
        isSurfaceOwner ? runtime.personalSuggestionService : nil
    }

    var correctionWords: CorrectionWordSource { runtime.correctionWords }

    /// Asks for the surface. While loading, a surface another controller is showing
    /// is left alone; a controller that is appearing takes it.
    func claimSurface(at moment: KeyboardSurfaceClaimPolicy.Moment) {
        runtime.surfaceHost.claim(for: self, at: moment, make: makePrimarySurface)
    }

    func bind(_ surface: KeyboardSurfaceView) {
        runtime.beginTypingSession(hasFullAccess: hasFullAccess)
        surface.onKeyEvent = { [weak self] event in
            self?.handleKeyEvent(event)
        }
        surface.setInputModeSwitch(
            target: self,
            action: #selector(handleInputModeList(from:with:))
        )
        installClipboard(on: surface)
        installPersonalSuggestions(on: surface)
        followSeedLanguage()
#if DEBUG
        touchDiagnosticsReporter = KeyboardTouchDiagnosticsReporter(surface: surface)
#endif
        // One pass for both: a surface arriving from another host app still shows
        // that app's layout and backdrop.
        surface.apply(presentation: currentPresentation, backgroundImage: cachedBackgroundImage)
    }

    /// The next host app's controller has the surface. `viewWillDisappear` usually
    /// comes first and has already done this, but it can come late or not at all,
    /// and a clipboard monitor left running would invite a paste on a keyboard this
    /// controller no longer knows the field of.
    func surrenderSurface() {
        endPresentation()
        shortcutsLoader.cancel()
        releaseAllSupplementarySurfaces()
        emojiCatalogCache.release()
#if DEBUG
        touchDiagnosticsReporter?.finish()
        touchDiagnosticsReporter = nil
#endif
    }
}
