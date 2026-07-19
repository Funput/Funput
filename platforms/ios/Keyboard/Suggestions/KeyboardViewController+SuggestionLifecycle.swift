import UIKit

extension KeyboardViewController {
    override func viewWillDisappear(_ animated: Bool) {
        super.viewWillDisappear(animated)
        clearPersonalSuggestions()
        flushPersonalSuggestions()
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        flushPersonalSuggestions()
    }
}
