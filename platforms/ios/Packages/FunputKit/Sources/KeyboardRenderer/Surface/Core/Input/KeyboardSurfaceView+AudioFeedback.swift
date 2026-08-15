#if canImport(UIKit)
import UIKit

extension KeyboardSurfaceView: UIInputViewAudioFeedback {
    public var enableInputClicksWhenVisible: Bool {
        presentation.isKeySoundEnabled
    }
}
#endif
