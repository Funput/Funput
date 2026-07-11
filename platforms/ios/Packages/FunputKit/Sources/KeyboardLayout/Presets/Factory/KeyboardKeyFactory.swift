import Foundation

func characterKey(_ character: Character) -> KeySpec {
    let label = String(character)
    return KeySpec(
        id: "character-\(character)",
        label: label,
        role: .character,
        shiftedLabel: label.uppercased()
    )
}

func specialKey(
    _ id: String,
    _ label: String,
    _ role: KeyRole,
    weight: CGFloat = 1,
    accessibilityLabel: String? = nil
) -> KeySpec {
    KeySpec(
        id: id,
        label: label,
        role: role,
        widthWeight: weight,
        accessibilityLabel: accessibilityLabel
    )
}

func characterRow(_ characters: String, inset: CGFloat = 0) -> KeyboardRow {
    KeyboardRow(keys: characters.map(characterKey), horizontalInsetUnits: inset)
}

func bottomCharacterRow() -> KeyboardRow {
    var keys = [specialKey("shift", "", .shift, weight: 1.5, accessibilityLabel: "Shift")]
    keys.append(contentsOf: "zxcvbnm".map(characterKey))
    keys.append(specialKey("backspace", "", .backspace, weight: 1.5, accessibilityLabel: "Xóa"))
    return KeyboardRow(keys: keys)
}

func standardSpaceKey(weight: CGFloat = 5.8) -> KeySpec {
    KeySpec(
        id: "space",
        label: "Tiếng Việt",
        role: .space,
        widthWeight: weight,
        accessibilityLabel: "Dấu cách. Vuốt để đổi Tiếng Việt và Tiếng Anh",
        horizontalSwipeAction: .toggleLanguage
    )
}

func asciiSpaceKey(weight: CGFloat) -> KeySpec {
    KeySpec(
        id: "space",
        label: "English",
        role: .space,
        widthWeight: weight,
        accessibilityLabel: "Dấu cách"
    )
}

func standardActionRow() -> KeyboardRow {
    KeyboardRow(keys: [
        specialKey("symbols", "?123", .symbols, weight: 1.7, accessibilityLabel: "Ký hiệu"),
        specialKey("comma", ",", .punctuation, accessibilityLabel: "Dấu phẩy"),
        standardSpaceKey(),
        specialKey("period", ".", .punctuation, accessibilityLabel: "Dấu chấm"),
        specialKey("enter", "", .enter, weight: 1.7, accessibilityLabel: "Enter"),
    ])
}
