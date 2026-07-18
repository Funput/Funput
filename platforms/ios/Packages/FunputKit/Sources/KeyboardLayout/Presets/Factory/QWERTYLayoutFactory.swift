func qwertyLayout(
    id: String,
    inputMethod: KeyboardInputMethod,
    leadingRows: [KeyboardRow] = [],
    actionKeys: [KeySpec],
    showsTelexHints: Bool = false,
    showsToolbar: Bool = true
) -> KeyboardLayout {
    let displaysTelexHints = showsTelexHints && inputMethod == .telex
    return KeyboardLayout(
        id: id,
        inputMethod: inputMethod,
        toolbar: showsToolbar ? .standard : nil,
        rows: leadingRows + [
            characterRow("qwertyuiop", showsTelexHints: displaysTelexHints),
            characterRow("asdfghjkl", inset: 0.5, showsTelexHints: displaysTelexHints),
            bottomCharacterRow(showsTelexHints: displaysTelexHints),
            KeyboardRow(keys: actionKeys),
        ]
    )
}
