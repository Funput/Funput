import KeyboardInput
import UIKit

extension KeyboardViewController {
    func handleSystemInputModeEvent(from source: UIView, event: UIEvent) {
        inputCoordinator.prepareForSystemInputModeChange()
        handleInputModeList(from: source, with: event)
    }
}
