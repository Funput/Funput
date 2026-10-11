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
    // iPad keys. Drawn by the layout only; nothing handles them yet.
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
