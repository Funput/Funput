public enum SymbolKeyboardLayouts {
    public static func primary(
        _ inputMethod: KeyboardInputMethod,
        secure: Bool = false
    ) -> KeyboardLayout {
        create(
            id: "symbols-primary",
            inputMethod: inputMethod,
            secure: secure,
            rows: [
                topNumberRow(.plainPunctuation, pageID: "primary"),
                symbolRow(page: "primary", row: 1, labels: SymbolPageContent.primaryRow1),
                symbolRow(page: "primary", row: 2, labels: SymbolPageContent.primaryRow2),
                bottomRow(
                    page: "primary",
                    switchRole: .moreSymbols,
                    switchLabel: "=\\<",
                    symbols: SymbolPageContent.primaryRow3
                ),
                actionRow(page: "primary", secure: secure),
            ]
        )
    }

    public static func secondary(
        _ inputMethod: KeyboardInputMethod,
        secure: Bool = false
    ) -> KeyboardLayout {
        create(
            id: "symbols-secondary",
            inputMethod: inputMethod,
            secure: secure,
            rows: [
                topNumberRow(.plainPunctuation, pageID: "secondary"),
                symbolRow(page: "secondary", row: 1, labels: SymbolPageContent.secondaryRow1),
                symbolRow(page: "secondary", row: 2, labels: SymbolPageContent.secondaryRow2),
                bottomRow(
                    page: "secondary",
                    switchRole: .symbols,
                    switchLabel: "?123",
                    symbols: SymbolPageContent.secondaryRow3
                ),
                actionRow(page: "secondary", secure: secure),
            ]
        )
    }

    private static func create(
        id: String,
        inputMethod: KeyboardInputMethod,
        secure: Bool,
        rows: [KeyboardRow]
    ) -> KeyboardLayout {
        KeyboardLayout(
            id: "\(id)-\(inputMethod.rawValue)\(secure ? "-secure" : "")",
            inputMethod: inputMethod,
            toolbar: secure ? nil : .standard,
            rows: rows
        )
    }

    private static func symbolRow(page: String, row: Int, labels: [String]) -> KeyboardRow {
        KeyboardRow(keys: labels.enumerated().map { symbolKey(page, row, $0.offset, $0.element) })
    }

    private static func bottomRow(
        page: String,
        switchRole: KeyRole,
        switchLabel: String,
        symbols: [String]
    ) -> KeyboardRow {
        let leading = specialKey(
            "switch-\(page)",
            switchLabel,
            switchRole,
            weight: 1.5,
            accessibilityLabel: "Chuyển trang ký hiệu"
        )
        let trailing = specialKey(
            "backspace-\(page)",
            "",
            .backspace,
            weight: 1.5,
            accessibilityLabel: "Xóa"
        )
        let middle = symbols.enumerated().map { symbolKey(page, 3, $0.offset, $0.element) }
        return KeyboardRow(keys: [leading] + middle + [trailing])
    }

    private static func actionRow(page: String, secure: Bool) -> KeyboardRow {
        KeyboardRow(keys: [
            specialKey("letters-\(page)", "ABC", .letters, weight: 1.7, accessibilityLabel: "Chữ cái"),
            specialKey("comma-\(page)", ",", .punctuation, accessibilityLabel: "Dấu phẩy"),
            symbolSpace(page: page, secure: secure),
            specialKey("period-\(page)", ".", .punctuation, accessibilityLabel: "Dấu chấm"),
            specialKey("enter-\(page)", "", .enter, weight: 1.7, accessibilityLabel: "Enter"),
        ])
    }

    private static func symbolSpace(page: String, secure: Bool) -> KeySpec {
        KeySpec(
            id: "space-\(page)",
            label: secure ? "English" : "Tiếng Việt",
            role: .space,
            widthWeight: 5.8,
            accessibilityLabel: secure ? "Dấu cách" : "Dấu cách. Vuốt để đổi ngôn ngữ",
            horizontalSwipeAction: secure ? nil : .toggleLanguage
        )
    }

    private static func symbolKey(_ page: String, _ row: Int, _ index: Int, _ label: String) -> KeySpec {
        KeySpec(id: "symbol-\(page)-\(row)-\(index)", label: label, role: .punctuation)
    }
}
