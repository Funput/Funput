import CoreGraphics

/// The pieces every iPad page is built from.
///
/// Every row of every page adds up to 13 units — ten keys plus a key and a half at each
/// end — so a key keeps its column from row to row and from page to page.
enum PadKeyFactory {
    static let side: CGFloat = 1.5
    static let wide: CGFloat = 2

    static func edgeKey(
        _ id: String,
        _ label: String,
        _ role: KeyRole,
        _ accessibility: String,
        weight: CGFloat = side
    ) -> KeySpec {
        specialKey("\(id)-pad", label, role, weight: weight, accessibilityLabel: accessibility)
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
        edgeKey("backspace", "", .backspace, "Xóa", weight: weight)
    }

    static func returnKey() -> KeySpec {
        edgeKey("enter", "", .enter, "Enter", weight: wide)
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
            edgeKey(id, switchLabel, switchRole, switchAccessibility),
            spaceKey(composes: composes),
            edgeKey("\(id)-right", switchLabel, switchRole, switchAccessibility),
            edgeKey("dismiss", "", .dismissKeyboard, "Ẩn bàn phím"),
        ])
    }
}
