enum KeyboardHapticType: Equatable, Sendable {
    case keyPress
    case space
    case control
    /// The keyboard became something else under the finger, such as the caret trackpad.
    case modeChange
    case delete
    case deleteRepeat
    case submit
}
