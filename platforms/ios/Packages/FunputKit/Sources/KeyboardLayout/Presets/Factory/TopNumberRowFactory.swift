enum TopNumberRowMode {
    case vniModifiers
    case plainCharacter
    case plainPunctuation
}

func topNumberRowForLetters(_ inputMethod: KeyboardInputMethod) -> KeyboardRow {
    topNumberRow(
        inputMethod == .vni ? .vniModifiers : .plainCharacter,
        pageID: "letters-\(inputMethod.rawValue)"
    )
}

func topNumberRow(_ mode: TopNumberRowMode, pageID: String) -> KeyboardRow {
    let hints = ["´", "`", "̉", "˜", "̣", "ˆ", "+", "˘", "đ", "×"]
    let descriptions = [
        "Dấu sắc", "Dấu huyền", "Dấu hỏi", "Dấu ngã", "Dấu nặng",
        "Dấu mũ", "Dấu móc", "Dấu trăng", "Chữ đ", "Xóa dấu",
    ]
    let keys = (0..<10).map { index -> KeySpec in
        let digit = index == 9 ? 0 : index + 1
        let label = String(digit)
        switch mode {
        case .vniModifiers:
            return KeySpec(
                id: "vni-\(pageID)-\(digit)",
                label: label,
                role: .vniModifier,
                secondaryLabel: hints[index],
                accessibilityLabel: descriptions[index]
            )
        case .plainCharacter:
            return KeySpec(id: "digit-\(pageID)-\(digit)", label: label, role: .character)
        case .plainPunctuation:
            return KeySpec(id: "digit-\(pageID)-\(digit)", label: label, role: .punctuation)
        }
    }
    return KeyboardRow(keys: keys)
}
