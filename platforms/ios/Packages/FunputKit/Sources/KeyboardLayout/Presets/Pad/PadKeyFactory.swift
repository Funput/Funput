import CoreGraphics

/// The pieces every iPad page is built from.
///
/// Every row of every page adds up to 13 units — ten keys plus a key and a half at each
/// end — so a key keeps its column from row to row and from page to page. Function keys are
/// written out in words in a bottom corner, as on the stock keyboard: the leading end of a
/// row on the left, the trailing end on the right. Only the globe and hide keys are icons.
enum PadKeyFactory {
    static let side: CGFloat = 1.5
    static let wide: CGFloat = 2
    /// The two shift keys share the four units `wide` gives both ends of the bottom letter row.
    /// The stock left shift is longer, which pushes `z` a little to the right of `a`.
    static let leadingShift: CGFloat = 2.3
    static let trailingShift: CGFloat = 1.7

    static func edgeKey(
        _ id: String,
        _ label: String,
        _ role: KeyRole,
        _ accessibility: String,
        weight: CGFloat = side,
        placement: KeyLabelPlacement = .center
    ) -> KeySpec {
        KeySpec(
            id: "\(id)-pad",
            label: label,
            role: role,
            widthWeight: weight,
            accessibilityLabel: accessibility,
            labelPlacement: placement
        )
    }

    /// A key at the left end of a row, its word in the bottom-left corner.
    static func leadingKey(
        _ id: String, _ label: String, _ role: KeyRole, _ accessibility: String, weight: CGFloat = side
    ) -> KeySpec {
        edgeKey(id, label, role, accessibility, weight: weight, placement: .bottomLeading)
    }

    /// A key at the right end of a row, its word in the bottom-right corner.
    static func trailingKey(
        _ id: String, _ label: String, _ role: KeyRole, _ accessibility: String, weight: CGFloat = side
    ) -> KeySpec {
        edgeKey(id, label, role, accessibility, weight: weight, placement: .bottomTrailing)
    }

    /// An empty slot that holds the columns in place.
    static func spacer(_ id: String, weight: CGFloat = side) -> KeySpec {
        KeySpec(
            id: "placeholder-pad-\(id)",
            label: "",
            role: .placeholder,
            widthWeight: weight,
            accessibilityLabel: "Vị trí trống"
        )
    }

    /// The digits, held to the letter columns by an empty key at each end.
    static func numberRow(_ digits: KeyboardRow) -> KeyboardRow {
        KeyboardRow(
            keys: [spacer("leading")] + digits.keys + [spacer("trailing")],
            isNumberRow: true
        )
    }

    static func deleteKey(weight: CGFloat = side) -> KeySpec {
        trailingKey("backspace", "delete", .backspace, "Xóa", weight: weight)
    }

    static func returnKey() -> KeySpec {
        trailingKey("enter", "return", .enter, "Enter", weight: wide)
    }

    /// The space bar: Vietnamese with the language swipe where the field composes, plain
    /// otherwise, as on the phone.
    static func spaceKey(composes: Bool) -> KeySpec {
        composes ? standardSpaceKey(weight: 7) : asciiSpaceKey(weight: 7)
    }

    /// `globe · switch · space · switch · hide`, with no dictation key.
    static func actionRow(
        switchLabel: String,
        switchRole: KeyRole,
        switchAccessibility: String,
        composes: Bool
    ) -> KeyboardRow {
        let id = switchRole == .letters ? "letters" : "symbols"
        return KeyboardRow(keys: [
            edgeKey("globe", "", .globe, "Đổi bàn phím"),
            leadingKey(id, switchLabel, switchRole, switchAccessibility),
            spaceKey(composes: composes),
            trailingKey("\(id)-right", switchLabel, switchRole, switchAccessibility),
            edgeKey("dismiss", "", .dismissKeyboard, "Ẩn bàn phím"),
        ])
    }
}
