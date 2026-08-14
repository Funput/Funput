import KeyboardConfiguration
import KeyboardRenderer
import ThemeSchema
import UIKit

/// One backdrop resolution: which asset, decoded for which keyboard width.
struct KeyboardBackgroundRequest: Equatable {
    let assetID: String?
    let pixelBudget: Int
}

extension KeyboardViewController {
    override func viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()
        refreshBackgroundImage()
    }

    /// Resolves the backdrop once the host has given the keyboard a width.
    ///
    /// Deliberately not part of bootstrap: `view.bounds` is still zero in
    /// `viewDidLoad`, and decoding without a target size is what produced the 20MB+
    /// bitmaps. The first layout pass lands about a millisecond later and hundreds of
    /// milliseconds before the keyboard is visible, so nothing is lost by waiting.
    ///
    /// Runs on every layout pass, so it has to be free when nothing changed — hence
    /// the request check rather than a cache probe. A decode that fails caches
    /// nothing, and without this guard each pass would read the asset off disk again.
    /// `adopt` clears the request, so a transient failure is still retried once per
    /// activation.
    func refreshBackgroundImage() {
        let budget = backgroundImagePixelBudget
        guard budget > 0 else { return }
        let request = KeyboardBackgroundRequest(
            assetID: selectedTheme.backgroundEffects.image?.assetID,
            pixelBudget: budget
        )
        guard resolvedBackgroundRequest != request else { return }
        resolvedBackgroundRequest = request

        let image = loadBackgroundImage(
            assetID: request.assetID,
            pixelBudget: request.pixelBudget
        )
        keyboardView.backgroundImage = image
        applyBackgroundImageToSupplementarySurfaces(image)
    }

    /// Drops the decoded backdrop when the keyboard is off screen.
    ///
    /// It is the single largest allocation the extension holds, and releasing panels
    /// alone left it in place. Guarded on visibility so a warning that arrives while
    /// the user is typing does not throw away an image the next layout pass would
    /// immediately decode again.
    func releaseBackgroundImageIfHidden() {
        guard !isKeyboardVisible else { return }
        backgroundImageCache.clear()
        resolvedBackgroundRequest = nil
        keyboardView.backgroundImage = nil
        applyBackgroundImageToSupplementarySurfaces(nil)
    }

    /// The keyboard spans the host's full width, so width alone bounds the backdrop.
    /// Height is deliberately ignored: it changes while the keyboard settles, and
    /// keying on it would decode the same asset twice on every launch.
    private var backgroundImagePixelBudget: Int {
        let width = view.bounds.width
        guard width > 0 else { return 0 }
        return Int((width * max(traitCollection.displayScale, 1)).rounded(.up))
    }
}
