public struct KeyboardToolbarSpec: Hashable, Sendable {
    public let clipboardKey: KeySpec
    public let emojiKey: KeySpec

    public var keys: [KeySpec] {
        [clipboardKey, emojiKey]
    }

    public init() {
        clipboardKey = KeySpec(
            id: "toolbar-clipboard",
            label: "",
            role: .clipboard,
            accessibilityLabel: "Lịch sử clipboard"
        )
        emojiKey = KeySpec(
            id: "toolbar-emoji",
            label: "",
            role: .emoji,
            accessibilityLabel: "Biểu tượng cảm xúc"
        )
    }

    public static var standard: Self {
        KeyboardToolbarSpec()
    }
}
