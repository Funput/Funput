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
    case systemInputMode
    case space
    case enter
    case emoji

    public var isSpecial: Bool {
        switch self {
        case .character, .vniModifier, .punctuation, .space:
            false
        default:
            true
        }
    }
}
