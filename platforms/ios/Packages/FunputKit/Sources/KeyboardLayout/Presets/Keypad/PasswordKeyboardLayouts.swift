public enum PasswordKeyboardLayouts {
    public static func text(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        qwertyLayout(
            id: "qwerty-password-\(inputMethod.rawValue)",
            inputMethod: inputMethod,
            leadingRows: [KeyboardRow(keys: "1234567890".map(keypadDigit))],
            actionKeys: [
                specialKey("symbols", "?123", .symbols, weight: 1.7, accessibilityLabel: "Ký hiệu"),
                specialKey("comma", ",", .punctuation, accessibilityLabel: "Dấu phẩy"),
                asciiSpaceKey(weight: 5.8),
                specialKey("period", ".", .punctuation, accessibilityLabel: "Dấu chấm"),
                specialKey("enter", "", .enter, weight: 1.7, accessibilityLabel: "Enter"),
            ],
            showsToolbar: false
        )
    }

    public static func pin(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        KeyboardLayout(
            id: "pin-\(inputMethod.rawValue)",
            inputMethod: inputMethod,
            toolbar: nil,
            rows: [
                keypadRow(keypadDigit("1"), keypadDigit("2"), keypadDigit("3"), backspace()),
                keypadRow(keypadDigit("4"), keypadDigit("5"), keypadDigit("6"), enter()),
                keypadRow(keypadDigit("7"), keypadDigit("8"), keypadDigit("9"), keypadEmpty("pin-top")),
                keypadRow(
                    keypadEmpty("pin-left"),
                    keypadDigit("0"),
                    keypadEmpty("pin-center"),
                    keypadEmpty("pin-right")
                ),
            ]
        )
    }

    private static func backspace() -> KeySpec {
        keypadCommand("backspace", role: .backspace, accessibilityLabel: "Xóa")
    }

    private static func enter() -> KeySpec {
        keypadCommand("enter", role: .enter, accessibilityLabel: "Enter")
    }
}
