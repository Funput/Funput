/// Puts the iPad's typing keys on the same bottom line as its function keys.
///
/// The stock keyboard sets `q` and `tab` on one line, with the digit or hint centered above
/// the letter. Its globe and emoji symbols sit in the bottom-left corner of their keys and
/// the hide-keyboard symbol in the bottom-right, on that same line. Running this last means
/// pages built from phone parts, and the compact digit hints, all get it without each
/// builder repeating the placement.
enum PadLabelPlacement {
    static func apply(to layout: KeyboardLayout) -> KeyboardLayout {
        KeyboardLayout(
            id: layout.id,
            inputMethod: layout.inputMethod,
            toolbar: layout.toolbar,
            rows: layout.rows.map(apply),
            heightRowCount: layout.heightRowCount
        )
    }

    static func placement(of key: KeySpec) -> KeyLabelPlacement {
        guard key.labelPlacement == .center else { return key.labelPlacement }
        switch key.role {
        case .character, .punctuation, .vniModifier: return .bottomCenter
        case .globe, .emoji: return .bottomLeading
        case .dismissKeyboard: return .bottomTrailing
        default: return key.labelPlacement
        }
    }

    private static func apply(to row: KeyboardRow) -> KeyboardRow {
        KeyboardRow(
            keys: row.keys.map(placed),
            horizontalInsetUnits: row.horizontalInsetUnits,
            isNumberRow: row.isNumberRow,
            columnSpans: row.columnSpans
        )
    }

    private static func placed(_ key: KeySpec) -> KeySpec {
        let target = placement(of: key)
        guard target != key.labelPlacement else { return key }
        return KeySpec(
            id: key.id,
            label: key.label,
            role: key.role,
            widthWeight: key.widthWeight,
            shiftedLabel: key.shiftedLabel,
            secondaryLabel: key.secondaryLabel,
            accessibilityLabel: key.accessibilityLabel,
            horizontalSwipeAction: key.horizontalSwipeAction,
            alternates: key.alternates,
            alternateColumns: key.alternateColumns,
            labelPlacement: target
        )
    }
}
