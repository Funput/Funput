func qwertyLayout(
    id: String,
    inputMethod: KeyboardInputMethod,
    leadingRows: [KeyboardRow] = [],
    actionKeys: [KeySpec],
    showsToolbar: Bool = true
) -> KeyboardLayout {
    KeyboardLayout(
        id: id,
        inputMethod: inputMethod,
        toolbar: showsToolbar ? .standard : nil,
        rows: leadingRows + [
            characterRow("qwertyuiop"),
            characterRow("asdfghjkl", inset: 0.5),
            bottomCharacterRow(),
            KeyboardRow(keys: actionKeys),
        ]
    )
}
