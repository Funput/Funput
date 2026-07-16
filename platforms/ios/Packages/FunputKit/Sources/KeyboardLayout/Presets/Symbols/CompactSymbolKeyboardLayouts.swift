enum CompactSymbolKeyboardLayouts {
    static func primary(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        create(
            id: "symbols-primary-\(inputMethod.rawValue)-compact",
            inputMethod: inputMethod,
            rows: [
                topNumberRow(.plainPunctuation, pageID: "compact-primary"),
                symbolRow(page: "compact-primary", labels: CompactSymbolPageContent.primaryMiddle),
                bottomRow(
                    page: "compact-primary",
                    switchRole: .moreSymbols,
                    switchLabel: "=\\<",
                    symbols: CompactSymbolPageContent.primaryBottom
                ),
                actionRow(page: "compact-primary"),
            ]
        )
    }

    static func secondary(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        create(
            id: "symbols-secondary-\(inputMethod.rawValue)-compact",
            inputMethod: inputMethod,
            rows: [
                symbolRow(page: "compact-secondary-top", labels: CompactSymbolPageContent.secondaryTop),
                symbolRow(page: "compact-secondary-middle", labels: CompactSymbolPageContent.secondaryMiddle),
                bottomRow(
                    page: "compact-secondary",
                    switchRole: .symbols,
                    switchLabel: "?123",
                    symbols: CompactSymbolPageContent.secondaryBottom
                ),
                actionRow(page: "compact-secondary"),
            ]
        )
    }

    private static func create(
        id: String,
        inputMethod: KeyboardInputMethod,
        rows: [KeyboardRow]
    ) -> KeyboardLayout {
        KeyboardLayout(id: id, inputMethod: inputMethod, toolbar: .standard, rows: rows)
    }

    private static func symbolRow(page: String, labels: [String]) -> KeyboardRow {
        KeyboardRow(keys: labels.enumerated().map { index, label in
            KeySpec(id: "symbol-\(page)-\(index)", label: label, role: .punctuation)
        })
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
        let middle = symbols.enumerated().map { index, label in
            KeySpec(id: "symbol-\(page)-bottom-\(index)", label: label, role: .punctuation)
        }
        let trailing = specialKey(
            "backspace-\(page)",
            "",
            .backspace,
            weight: 1.5,
            accessibilityLabel: "Xóa"
        )
        return KeyboardRow(keys: [leading] + middle + [trailing])
    }

    private static func actionRow(page: String) -> KeyboardRow {
        KeyboardRow(keys: [
            specialKey("letters-\(page)", "ABC", .letters, weight: 1.7, accessibilityLabel: "Chữ cái"),
            specialKey("comma-\(page)", ",", .punctuation, accessibilityLabel: "Dấu phẩy"),
            standardSpaceKey(weight: 5.8),
            specialKey("period-\(page)", ".", .punctuation, accessibilityLabel: "Dấu chấm"),
            specialKey("enter-\(page)", "", .enter, weight: 1.7, accessibilityLabel: "Enter"),
        ])
    }
}
