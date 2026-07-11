public enum PhoneKeyboardLayouts {
    public static func resolve(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        KeyboardLayout(
            id: "phone-\(inputMethod.rawValue)",
            inputMethod: inputMethod,
            toolbar: nil,
            rows: [
                keypadRow(keypadDigit("1"), keypadDigit("2"), keypadDigit("3"), backspace()),
                keypadRow(keypadDigit("4"), keypadDigit("5"), keypadDigit("6"), enter()),
                keypadRow(
                    keypadDigit("7"),
                    keypadDigit("8"),
                    keypadDigit("9"),
                    keypadText("plus", value: "+", accessibilityLabel: "Dấu cộng")
                ),
                keypadRow(
                    keypadText("star", value: "*", accessibilityLabel: "Dấu sao"),
                    keypadDigit("0"),
                    keypadText("hash", value: "#", accessibilityLabel: "Dấu thăng"),
                    keypadEmpty("phone")
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
