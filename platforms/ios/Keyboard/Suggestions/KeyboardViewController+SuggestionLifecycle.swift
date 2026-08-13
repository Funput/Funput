import KeyboardConfiguration
import UIKit

extension KeyboardViewController {
    override func viewWillDisappear(_ animated: Bool) {
        activationState.end()
        cancelClipboardRetry()
        deactivatePreferredHeight()
        super.viewWillDisappear(animated)
        clearPersonalSuggestions()
        flushPersonalSuggestions()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        flushPersonalSuggestions()
        releaseHiddenSupplementarySurfaces()
    }
}
