public struct KeyboardToolbarSpec: Hashable, Sendable {
    public let emojiKey: KeySpec

    public var keys: [KeySpec] {
        [emojiKey]
    }

    public init() {
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
