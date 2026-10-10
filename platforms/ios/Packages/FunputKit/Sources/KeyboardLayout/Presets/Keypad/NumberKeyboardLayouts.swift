import Foundation

public enum NumberKeyboardLayouts {
    /// Resolves the numeric keypad for a field.
    ///
    /// Mirrors the stock iOS number pad: three columns, digits 1-9, and a bottom row of
    /// `[extra] 0 ⌫`. There is no Return key (the system number pad has none either), so the
    /// digits keep the full width instead of leaving dead cells around them.
    ///
    /// The one exception is signed + decimal, which needs two extra keys beside `0` and so
    /// cannot fit the three-column shape while still keeping Delete reachable.
    public static func resolve(
        _ inputMethod: KeyboardInputMethod,
        mode: KeyboardEditorMode,
        decimalSeparator: String = Locale.current.decimalSeparator ?? "."
    ) -> KeyboardLayout {
        precondition(mode.isNumber, "Number layout requires a numeric editor mode")
        return KeyboardLayout(
            id: "number-\(mode.rawValue)-\(inputMethod.rawValue)",
            inputMethod: inputMethod,
            toolbar: nil,
            rows: rows(mode, decimalSeparator: decimalSeparator)
        )
    }

    private static func rows(
        _ mode: KeyboardEditorMode,
        decimalSeparator: String
    ) -> [KeyboardRow] {
        if mode.allowsSigned && mode.allowsDecimal {
            return [
                keypadRow(keypadDigit("1"), keypadDigit("2"), keypadDigit("3"), backspace()),
                keypadRow(keypadDigit("4"), keypadDigit("5"), keypadDigit("6"), enter()),
                keypadRow(keypadDigit("7"), keypadDigit("8"), keypadDigit("9"), period()),
                keypadRow(sign(), keypadDigit("0"), keypadEmpty("center"), comma()),
            ]
        }
        return [
            keypadRow(keypadDigit("1"), keypadDigit("2"), keypadDigit("3")),
            keypadRow(keypadDigit("4"), keypadDigit("5"), keypadDigit("6")),
            keypadRow(keypadDigit("7"), keypadDigit("8"), keypadDigit("9")),
            keypadRow(
                bottomLeft(mode, decimalSeparator: decimalSeparator),
                keypadDigit("0"),
                backspace()
            ),
        ]
    }

    private static func bottomLeft(
        _ mode: KeyboardEditorMode,
        decimalSeparator: String
    ) -> KeySpec {
        if mode.allowsSigned { return sign() }
        if mode.allowsDecimal {
            return keypadText(
                "decimal",
                value: decimalSeparator,
                accessibilityLabel: decimalSeparator == "," ? "Dấu phẩy thập phân" : "Dấu thập phân"
            )
        }
        return keypadEmpty("left")
    }

    private static func backspace() -> KeySpec {
        keypadCommand("backspace", role: .backspace, accessibilityLabel: "Xóa")
    }

    private static func enter() -> KeySpec {
        keypadCommand("enter", role: .enter, accessibilityLabel: "Enter")
    }

    private static func sign() -> KeySpec {
        keypadText("minus", value: "-", accessibilityLabel: "Dấu trừ")
    }

    private static func period() -> KeySpec {
        keypadText("period", value: ".", accessibilityLabel: "Dấu thập phân")
    }

    private static func comma() -> KeySpec {
        keypadText("comma", value: ",", accessibilityLabel: "Dấu phẩy thập phân")
    }
}
