import KeyboardLayout

/// Which of a style's three sounds a key plays, split the way the system keyboard
/// splits its own clicks: typing, deleting, and everything that changes mode.
public enum KeySoundKind: String, CaseIterable, Hashable, Sendable {
    case key
    case delete
    case modifier

    /// `nil` for keys that make no sound at all.
    public init?(role: KeyRole) {
        switch role {
        case .placeholder:
            return nil
        case .character, .vniModifier, .punctuation:
            self = .key
        case .backspace:
            self = .delete
        case .shift, .symbols, .moreSymbols, .letters, .inputMethod, .space, .enter,
             .emoji, .clipboard, .tab, .capsLock, .globe, .dismissKeyboard:
            self = .modifier
        }
    }
}
