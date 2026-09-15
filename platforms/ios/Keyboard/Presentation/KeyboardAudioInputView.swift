import UIKit

/// UIKit checks the registered input view when enabling `playInputClick()`.
/// Conformance on a nested keyboard surface alone does not enable input clicks.
final class KeyboardAudioInputView: UIInputView, UIInputViewAudioFeedback {
    var isKeySoundEnabled = false

    var enableInputClicksWhenVisible: Bool {
        isKeySoundEnabled
    }
}
