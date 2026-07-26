func qwertyLayout(
    id: String,
    inputMethod: KeyboardInputMethod,
    leadingRows: [KeyboardRow] = [],
    actionKeys: [KeySpec],
    showsTelexHints: Bool = false,
    supportsVietnameseAlternates: Bool = false,
    showsToolbar: Bool = true
) -> KeyboardLayout {
    let displaysTelexHints = showsTelexHints && inputMethod.isTelexFamily
    return KeyboardLayout(
        id: id,
        inputMethod: inputMethod,
        toolbar: showsToolbar ? .standard : nil,
        rows: leadingRows + [
            characterRow(
                "qwertyuiop",
                showsTelexHints: displaysTelexHints,
                supportsVietnameseAlternates: supportsVietnameseAlternates
            ),
            characterRow(
                "asdfghjkl",
                inset: 0.5,
                showsTelexHints: displaysTelexHints,
                supportsVietnameseAlternates: supportsVietnameseAlternates
            ),
            bottomCharacterRow(
                showsTelexHints: displaysTelexHints,
                supportsVietnameseAlternates: supportsVietnameseAlternates
            ),
            KeyboardRow(keys: actionKeys),
        ]
    )
}
