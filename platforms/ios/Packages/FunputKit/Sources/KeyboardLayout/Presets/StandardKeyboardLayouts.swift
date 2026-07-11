public enum StandardKeyboardLayouts {
    public static func letters(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        qwertyLayout(
            id: "qwerty-\(inputMethod.rawValue)",
            inputMethod: inputMethod,
            leadingRows: [topNumberRowForLetters(inputMethod)],
            actionKeys: standardActionRow().keys
        )
    }
}

public extension KeyboardLayout {
    static let funputQWERTY = StandardKeyboardLayouts.letters(.telex)
}
