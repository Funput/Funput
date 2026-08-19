public enum StandardKeyboardLayouts {
    public static func letters(
        _ inputMethod: KeyboardInputMethod,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        let hasNumberRow = inputMethod == .vni || showsNumberRow
        let layout = qwertyLayout(
            id: "qwerty-\(inputMethod.rawValue)\(hasNumberRow ? "" : "-compact")",
            inputMethod: inputMethod,
            leadingRows: hasNumberRow ? [topNumberRowForLetters(inputMethod)] : [],
            actionKeys: standardActionRow().keys,
            showsTelexHints: inputMethod.isTelexFamily,
            supportsVietnameseAlternates: true
        )
        // Digits move onto the top row only when no row of their own is on screen.
        guard !hasNumberRow, inputMethod.isTelexFamily else { return layout }
        return CompactDigitAlternates.decorate(layout)
    }
}

public extension KeyboardLayout {
    static let funputQWERTY = StandardKeyboardLayouts.letters(.telex)
}
