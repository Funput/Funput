import KeyboardLayout
import UIKit

extension KeyboardViewController {
    /// The iPad keys that act on the keyboard itself rather than on the document.
    ///
    /// A touch on the globe normally never gets here: the surface hands it to
    /// `handleInputModeList(from:with:)` so a hold can open the keyboard list. This path is
    /// what VoiceOver's activation reaches, and it moves to the next keyboard.
    func handleSystemKey(_ role: KeyRole) -> Bool {
        switch role {
        case .globe:
            advanceToNextInputMode()
        case .dismissKeyboard:
            dismissKeyboard()
        default:
            return false
        }
        return true
    }
}
