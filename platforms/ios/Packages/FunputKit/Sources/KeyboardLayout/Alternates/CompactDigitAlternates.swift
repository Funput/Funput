/// Digit hints and long-press digits for the top character row.
///
/// The compact letters page hides the number row, which leaves the symbols page as the
/// only way to reach a digit. Printing the digit on the keycap and offering it as the
/// pre-selected long-press alternate restores that reach without spending a row.
public enum CompactDigitAlternates {
    private static let rowCharacters = "qwertyuiop"
    private static let digits = "1234567890"

    public static func decorate(_ layout: KeyboardLayout) -> KeyboardLayout {
        KeyboardLayout(
            id: layout.id,
            inputMethod: layout.inputMethod,
            toolbar: layout.toolbar,
            rows: layout.rows.map(decorate)
        )
    }

    /// Matches the row by its own labels rather than by index: the compact page has no
    /// number row in front of it, so the top character row is not at a fixed position.
    private static func decorate(_ row: KeyboardRow) -> KeyboardRow {
        guard row.keys.map(\.label) == rowCharacters.map(String.init) else { return row }
        return KeyboardRow(
            keys: zip(row.keys, digits).map { key, digit in apply(String(digit), to: key) },
            horizontalInsetUnits: row.horizontalInsetUnits
        )
    }

    private static func apply(_ digit: String, to key: KeySpec) -> KeySpec {
        KeySpec(
            id: key.id,
            label: key.label,
            role: key.role,
            widthWeight: key.widthWeight,
            shiftedLabel: key.shiftedLabel,
            // A Telex tone hint keeps the slot it already owns — `r` stays the hỏi key —
            // and the digit joins it on the right, where every other key carries one.
            secondaryLabel: key.secondaryLabel.map { "\($0) \(digit)" } ?? digit,
            accessibilityLabel: "\(key.accessibilityLabel), số \(digit)",
            horizontalSwipeAction: key.horizontalSwipeAction,
            // First place wins the palette's default selection, which is what makes a
            // plain hold-and-release type the digit.
            alternates: [KeyAlternate(text: digit, accessibilityLabel: "Số \(digit)")]
                + key.alternates
        )
    }
}
