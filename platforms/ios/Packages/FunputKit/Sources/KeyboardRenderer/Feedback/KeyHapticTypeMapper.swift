import KeyboardLayout

enum KeyHapticTypeMapper {
    static func map(_ role: KeyRole) -> KeyboardHapticType? {
        switch role {
        case .character, .vniModifier, .punctuation:
            .keyPress
        case .space:
            .space
        case .backspace:
            .delete
        case .enter:
            .submit
        case .placeholder:
            nil
        default:
            .control
        }
    }
}
