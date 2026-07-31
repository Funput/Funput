#if canImport(UIKit)
import KeyboardLayout
import KeyboardTouchUIKit

/// Translates arbiter emissions into the semantic events the controller consumes.
extension KeyboardTouchAction {
    var keyEvent: KeyboardKeyEvent {
        switch self {
        case let .released(hit):
            KeyboardKeyEvent(key: hit.key, phase: .released)
        case let .repeated(hit):
            KeyboardKeyEvent(key: hit.key, phase: .repeated)
        case let .alternate(hit, alternate):
            KeyboardKeyEvent(key: hit.key, phase: .alternateSelected(alternate))
        case let .swiped(hit, action):
            KeyboardKeyEvent(key: hit.key, phase: .swiped(action))
        case let .cancelled(hit):
            KeyboardKeyEvent(key: hit.key, phase: .cancelled)
        }
    }
}

extension KeyRole {
    /// Control keys change presentation instead of producing text.
    var isControl: Bool {
        switch self {
        case .character, .vniModifier, .punctuation, .space, .backspace:
            false
        default:
            true
        }
    }
}
#endif
