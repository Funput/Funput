func keypadRow(_ keys: KeySpec...) -> KeyboardRow {
    KeyboardRow(keys: keys)
}

func keypadDigit(_ value: Character) -> KeySpec {
    let label = String(value)
    return KeySpec(id: "keypad-digit-\(value)", label: label, role: .character)
}

func keypadCommand(_ id: String, role: KeyRole, accessibilityLabel: String) -> KeySpec {
    KeySpec(id: id, label: "", role: role, accessibilityLabel: accessibilityLabel)
}

func keypadText(_ id: String, value: String, accessibilityLabel: String) -> KeySpec {
    KeySpec(id: id, label: value, role: .punctuation, accessibilityLabel: accessibilityLabel)
}

func keypadEmpty(_ position: String) -> KeySpec {
    KeySpec(
        id: "placeholder-\(position)",
        label: "",
        role: .placeholder,
        accessibilityLabel: "Vị trí trống"
    )
}
