enum TopNumberRowMode {
    case vniModifiers
    case plainCharacter
    case plainPunctuation
}

func topNumberRowForLetters(_ inputMethod: KeyboardInputMethod) -> KeyboardRow {
    topNumberRow(for: inputMethod, pageID: "letters-\(inputMethod.rawValue)")
}

/// The digit row for any page that composes Vietnamese, so VNI gets its tone modifiers
/// and their hints wherever composition is live — not only on the letters page.
func topNumberRow(for inputMethod: KeyboardInputMethod, pageID: String) -> KeyboardRow {
    topNumberRow(inputMethod == .vni ? .vniModifiers : .plainCharacter, pageID: pageID)
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
