public enum NumberKeyboardLayouts {
    public static func resolve(
        _ inputMethod: KeyboardInputMethod,
        mode: KeyboardEditorMode
    ) -> KeyboardLayout {
        precondition(mode.isNumber, "Number layout requires a numeric editor mode")
        return KeyboardLayout(
            id: "number-\(mode.rawValue)-\(inputMethod.rawValue)",
            inputMethod: inputMethod,
            toolbar: nil,
            rows: [
                keypadRow(keypadDigit("1"), keypadDigit("2"), keypadDigit("3"), backspace()),
                keypadRow(keypadDigit("4"), keypadDigit("5"), keypadDigit("6"), enter()),
                keypadRow(keypadDigit("7"), keypadDigit("8"), keypadDigit("9"), period(mode)),
                keypadRow(sign(mode), keypadDigit("0"), keypadEmpty("center"), comma(mode)),
            ]
        )
    }

    private static func backspace() -> KeySpec {
        keypadCommand("backspace", role: .backspace, accessibilityLabel: "Xóa")
    }

    private static func enter() -> KeySpec {
        keypadCommand("enter", role: .enter, accessibilityLabel: "Enter")
    }

    private static func sign(_ mode: KeyboardEditorMode) -> KeySpec {
        mode.allowsSigned
            ? keypadText("minus", value: "-", accessibilityLabel: "Dấu trừ")
            : keypadEmpty("sign")
    }

    private static func period(_ mode: KeyboardEditorMode) -> KeySpec {
        mode.allowsDecimal
            ? keypadText("period", value: ".", accessibilityLabel: "Dấu thập phân")
            : keypadEmpty("period")
    }

    private static func comma(_ mode: KeyboardEditorMode) -> KeySpec {
        mode.allowsDecimal
            ? keypadText("comma", value: ",", accessibilityLabel: "Dấu phẩy thập phân")
            : keypadEmpty("comma")
    }
}
