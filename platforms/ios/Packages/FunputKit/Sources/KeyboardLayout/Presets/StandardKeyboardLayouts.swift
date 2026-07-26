public enum StandardKeyboardLayouts {
    public static func letters(
        _ inputMethod: KeyboardInputMethod,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        let hasNumberRow = inputMethod == .vni || showsNumberRow
        return qwertyLayout(
            id: "qwerty-\(inputMethod.rawValue)\(hasNumberRow ? "" : "-compact")",
            inputMethod: inputMethod,
            leadingRows: hasNumberRow ? [topNumberRowForLetters(inputMethod)] : [],
            actionKeys: standardActionRow().keys,
            showsTelexHints: inputMethod.isTelexFamily,
            supportsVietnameseAlternates: true
        )
    }
}

public extension KeyboardLayout {
    static let funputQWERTY = StandardKeyboardLayouts.letters(.telex)
}
