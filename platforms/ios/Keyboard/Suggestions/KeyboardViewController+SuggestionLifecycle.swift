import FunputShared
import KeyboardConfiguration
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    override func viewWillDisappear(_ animated: Bool) {
        endPresentation()
        super.viewWillDisappear(animated)
    }

    /// Ends this controller's turn as the keyboard. Reached from `viewWillDisappear`
    /// and from losing the surface to the next host app, in either order and possibly
    /// more than once, so every step is safe to repeat.
    func endPresentation() {
        activationState.end()
        stopClipboardMonitoring()
        cancelClipboardRetry()
        markPreferredHeightHidden()
        clearPersonalSuggestions()
        flushPersonalSuggestions()
        reportCorrectionSession()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        flushPersonalSuggestions()
        releaseHiddenSupplementarySurfaces()
        emojiCatalogCache.release()
        releaseBackgroundImageIfHidden()
        KeyboardMemoryTrace.record("MemoryWarning")
    }
}
