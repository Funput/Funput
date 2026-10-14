/// The symbols the stock iPad keyboard prints above its letters, beside the digits on the top
/// row, and offers as the pre-selected long-press alternate.
///
/// Only the compact page has them: with a number row the symbols page is one tap away and
/// the stock keyboard leaves the letters bare.
enum PadSymbolHints {
    /// Letters of a row, in order, and the symbol over each.
    private static let rows: [(letters: String, symbols: [String])] = [
        ("asdfghjkl", ["@", "#", "$", "&", "*", "(", ")", "'", "\""]),
        ("zxcvbnm", ["%", "-", "+", "=", "/", ";", ":"]),
    ]

    static func decorate(_ layout: KeyboardLayout) -> KeyboardLayout {
        KeyboardLayout(
            id: layout.id,
            inputMethod: layout.inputMethod,
            toolbar: layout.toolbar,
            rows: layout.rows.map(decorate),
            heightRowCount: layout.heightRowCount
        )
    }

    /// Matches a row by the letters it holds, so the keys at either end do not matter.
    private static func decorate(_ row: KeyboardRow) -> KeyboardRow {
        let letters = row.keys.filter { $0.role == .character }.map(\.label).joined()
        guard let match = rows.first(where: { $0.letters == letters }) else { return row }
        var remaining = match.symbols.makeIterator()
        return KeyboardRow(
            keys: row.keys.map { key in
                guard key.role == .character, let symbol = remaining.next() else { return key }
                return apply(symbol, to: key)
            },
            horizontalInsetUnits: row.horizontalInsetUnits,
            isNumberRow: row.isNumberRow,
            columnSpans: row.columnSpans
        )
    }

    private static func apply(_ symbol: String, to key: KeySpec) -> KeySpec {
        KeySpec(
            id: key.id,
            label: key.label,
            role: key.role,
            widthWeight: key.widthWeight,
            shiftedLabel: key.shiftedLabel,
            secondaryLabel: symbol,
            accessibilityLabel: "\(key.accessibilityLabel), ký hiệu \(symbol)",
            horizontalSwipeAction: key.horizontalSwipeAction,
            // First place wins the palette's default selection, so hold-and-release types the symbol.
            alternates: [KeyAlternate(text: symbol, accessibilityLabel: "Ký hiệu \(symbol)")]
                + key.alternates,
            alternateColumns: key.alternateColumns,
            labelPlacement: key.labelPlacement
        )
    }
}
