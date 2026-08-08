/// The two symbol pages of the system preset, mirroring Apple's Vietnamese keyboard.
///
/// Both pages are four rows regardless of the number-row preference: page one always
/// carries the digits, so there is no compact variant to choose between.
public enum SystemSymbolKeyboardLayouts {
    public static func primary(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        create(
            id: "symbols-primary-\(inputMethod.rawValue)-system",
            inputMethod: inputMethod,
            rows: [
                topNumberRow(.plainPunctuation, pageID: "system-primary"),
                symbolRow(page: "system-primary", labels: SystemSymbolPageContent.primaryRow),
                punctuationRow(
                    page: "system-primary",
                    switchLabel: "#+=",
                    switchRole: .moreSymbols
                ),
                systemSymbolsActionRow(page: "system-primary"),
            ]
        )
    }

    public static func secondary(_ inputMethod: KeyboardInputMethod) -> KeyboardLayout {
        create(
            id: "symbols-secondary-\(inputMethod.rawValue)-system",
            inputMethod: inputMethod,
            rows: [
                symbolRow(
                    page: "system-secondary-upper",
                    labels: SystemSymbolPageContent.secondaryRowUpper
                ),
                symbolRow(
                    page: "system-secondary-lower",
                    labels: SystemSymbolPageContent.secondaryRowLower
                ),
                punctuationRow(
                    page: "system-secondary",
                    switchLabel: "123",
                    switchRole: .symbols
                ),
                systemSymbolsActionRow(page: "system-secondary"),
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

    /// `<switch> . , ? ! ' <backspace>` — five middle keys rather than the seven the
    /// Funput preset uses, so they render wider. That is what the stock keyboard does.
    private static func punctuationRow(
        page: String,
        switchLabel: String,
        switchRole: KeyRole
    ) -> KeyboardRow {
        let leading = specialKey(
            "switch-\(page)",
            switchLabel,
            switchRole,
            weight: 1.5,
            accessibilityLabel: "Chuyển trang ký hiệu"
        )
        let middle = SystemSymbolPageContent.punctuationRow.enumerated().map { index, label in
            KeySpec(id: "punctuation-\(page)-\(index)", label: label, role: .punctuation)
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
}
