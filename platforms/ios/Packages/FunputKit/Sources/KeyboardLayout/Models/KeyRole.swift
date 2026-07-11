public enum KeyRole: String, Hashable, Sendable {
    case character
    case punctuation
    case shift
    case backspace
    case symbols
    case inputMode
    case space
    case enter
    case emoji
    case settings

    public var isSpecial: Bool {
        switch self {
        case .character, .punctuation, .space:
            false
        default:
            true
        }
    }
}
