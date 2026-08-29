public enum StandardKeyboardLayouts {
    public static func letters(
        _ inputMethod: KeyboardInputMethod,
        showsNumberRow: Bool = true
    ) -> KeyboardLayout {
        let isCompact = usesCompactLetterRows(
            inputMethod: inputMethod,
            editorMode: .text,
            showsNumberRow: showsNumberRow
        )
        let layout = qwertyLayout(
            id: "qwerty-\(inputMethod.rawValue)\(isCompact ? "-compact" : "")",
            inputMethod: inputMethod,
            leadingRows: isCompact ? [] : [topNumberRowForLetters(inputMethod)],
            actionKeys: standardActionRow().keys,
            showsTelexHints: inputMethod.isTelexFamily,
            supportsVietnameseAlternates: true
        )
        // Digits move onto the top row only when no row of their own is on screen.
        return isCompact ? CompactDigitAlternates.decorate(layout) : layout
    }
}

public extension KeyboardLayout {
    static let funputQWERTY = StandardKeyboardLayouts.letters(.telex)
}
