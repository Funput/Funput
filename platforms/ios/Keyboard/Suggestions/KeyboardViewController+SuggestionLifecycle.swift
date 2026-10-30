import FunputShared
import KeyboardConfiguration
import KeyboardRenderer
import UIKit

extension KeyboardViewController {
    override func viewWillDisappear(_ animated: Bool) {
        activationState.end()
        stopClipboardMonitoring()
        cancelClipboardRetry()
        markPreferredHeightHidden()
        super.viewWillDisappear(animated)
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
