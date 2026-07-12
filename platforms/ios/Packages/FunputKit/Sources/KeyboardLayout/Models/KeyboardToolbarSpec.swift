public struct KeyboardToolbarSpec: Hashable, Sendable {
    public let systemInputModeKey: KeySpec?
    public let emojiKey: KeySpec

    public var keys: [KeySpec] {
        [systemInputModeKey, emojiKey].compactMap { $0 }
    }

    public init(systemInputModeKey: KeySpec? = nil) {
        self.systemInputModeKey = systemInputModeKey
        emojiKey = KeySpec(
            id: "toolbar-emoji",
            label: "",
            role: .emoji,
            accessibilityLabel: "Biểu tượng cảm xúc"
        )
    }

    /// Toolbar without the system keyboard switcher (single active keyboard).
    public static var standard: Self {
        KeyboardToolbarSpec()
    }

    /// Toolbar including the globe key, shown when more keyboards are installed.
    public static var withSystemInputMode: Self {
        KeyboardToolbarSpec(
            systemInputModeKey: KeySpec(
                id: "toolbar-system-input-mode",
                label: "",
                role: .systemInputMode,
                accessibilityLabel: "Chuyển bàn phím"
            )
        )
    }
}
