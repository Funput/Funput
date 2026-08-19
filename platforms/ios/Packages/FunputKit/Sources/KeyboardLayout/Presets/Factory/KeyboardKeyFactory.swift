import Foundation

private let telexKeyHints: [Character: (glyph: String, description: String)] = [
    "s": ("´", "dấu sắc"),
    "f": ("`", "dấu huyền"),
    "r": ("̉", "dấu hỏi"),
    "x": ("˜", "dấu ngã"),
    "j": ("̣", "dấu nặng"),
    "z": ("×", "xóa dấu"),
]

func characterKey(
    _ character: Character,
    showsTelexHint: Bool = false,
    supportsVietnameseAlternates: Bool = false
) -> KeySpec {
    let label = String(character)
    let hint = showsTelexHint ? telexKeyHints[character] : nil
    return KeySpec(
        id: "character-\(character)",
        label: label,
        role: .character,
        shiftedLabel: label.uppercased(),
        secondaryLabel: hint?.glyph,
        accessibilityLabel: hint.map { "\(label.uppercased()), \($0.description)" },
        alternates: supportsVietnameseAlternates
            ? VietnameseKeyAlternates.values(for: character) : []
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

func characterRow(
    _ characters: String,
    inset: CGFloat = 0,
    showsTelexHints: Bool = false,
    supportsVietnameseAlternates: Bool = false
) -> KeyboardRow {
    KeyboardRow(
        keys: characters.map {
            characterKey(
                $0,
                showsTelexHint: showsTelexHints,
                supportsVietnameseAlternates: supportsVietnameseAlternates
            )
        },
        horizontalInsetUnits: inset
    )
}

func bottomCharacterRow(
    showsTelexHints: Bool = false,
    supportsVietnameseAlternates: Bool = false
) -> KeyboardRow {
    var keys = [specialKey("shift", "", .shift, weight: 1.5, accessibilityLabel: "Shift")]
    keys.append(contentsOf: "zxcvbnm".map {
        characterKey(
            $0,
            showsTelexHint: showsTelexHints,
            supportsVietnameseAlternates: supportsVietnameseAlternates
        )
    })
    keys.append(specialKey("backspace", "", .backspace, weight: 1.5, accessibilityLabel: "Xóa"))
    return KeyboardRow(keys: keys)
}

func standardSpaceKey(weight: CGFloat = 5.8) -> KeySpec {
    KeySpec(
        id: "space",
        label: "Tiếng Việt",
        role: .space,
        widthWeight: weight,
        accessibilityLabel: "Dấu cách. Vuốt để đổi Tiếng Việt và Tiếng Anh. "
            + "Giữ rồi kéo để di chuyển con trỏ",
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
