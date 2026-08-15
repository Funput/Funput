import UIKit

extension KeyboardViewController {
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        launchTrace.recordFirstLayout(size: view.bounds.size)
    }
}
