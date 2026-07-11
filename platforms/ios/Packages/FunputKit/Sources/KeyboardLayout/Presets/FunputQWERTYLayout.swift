import Foundation

public extension KeyboardLayout {
    static let funputQWERTY = KeyboardLayout(
        id: "funput-qwerty",
        rows: [
            digitRow(),
            characterRow("qwertyuiop"),
            characterRow("asdfghjkl", inset: 0.5),
            bottomCharacterRow(),
            actionRow(),
        ]
    )
}

private func digitRow() -> KeyboardRow {
    let digits = ["1", "2", "3", "4", "5", "6", "7", "8", "9", "0"]
    return KeyboardRow(keys: digits.map { digit in
        KeySpec(id: "digit-\(digit)", label: digit, role: .character)
    })
}

private func characterRow(_ characters: String, inset: CGFloat = 0) -> KeyboardRow {
    KeyboardRow(keys: characters.map(characterKey), horizontalInsetUnits: inset)
}

private func bottomCharacterRow() -> KeyboardRow {
    var keys = [
        KeySpec(
            id: "shift",
            label: "",
            role: .shift,
            widthWeight: 1.45,
            accessibilityLabel: "Shift"
        ),
    ]
    keys.append(contentsOf: "zxcvbnm".map(characterKey))
    keys.append(
        KeySpec(
            id: "backspace",
            label: "",
            role: .backspace,
            widthWeight: 1.45,
            accessibilityLabel: "Xóa"
        )
    )
    return KeyboardRow(keys: keys)
}

private func actionRow() -> KeyboardRow {
    KeyboardRow(keys: [
        KeySpec(
            id: "symbols",
            label: "123",
            role: .symbols,
            widthWeight: 1.7,
            accessibilityLabel: "Số và ký hiệu"
        ),
        KeySpec(id: "comma", label: ",", role: .punctuation, accessibilityLabel: "Dấu phẩy"),
        KeySpec(
            id: "space",
            label: "Tiếng Việt",
            role: .space,
            widthWeight: 5.8,
            accessibilityLabel: "Dấu cách"
        ),
        KeySpec(id: "period", label: ".", role: .punctuation, accessibilityLabel: "Dấu chấm"),
        KeySpec(
            id: "enter",
            label: "",
            role: .enter,
            widthWeight: 1.7,
            accessibilityLabel: "Xuống dòng"
        ),
    ])
}

private func characterKey(_ character: Character) -> KeySpec {
    let label = String(character)
    return KeySpec(
        id: "character-\(character)",
        label: label,
        role: .character,
        shiftedLabel: label.uppercased()
    )
}
