public enum KeyRole: String, Hashable, Sendable {
    case placeholder
    case character
    case vniModifier
    case punctuation
    case shift
    case backspace
    case symbols
    case moreSymbols
    case letters
    case inputMethod
    case space
    case enter
    case emoji
    case clipboard
    // iPad keys. Tab and caps lock go through the coordinator; the globe and hide keys act
    // on the keyboard itself and are handled by the input view controller.
    case tab
    case capsLock
    case globe
    case dismissKeyboard

    public var isSpecial: Bool {
        switch self {
        case .character, .vniModifier, .punctuation, .space:
            false
        default:
            true
        }
    }
}
