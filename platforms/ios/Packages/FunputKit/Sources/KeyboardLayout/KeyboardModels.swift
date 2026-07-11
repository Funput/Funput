import Foundation

public enum KeyRole: String, Hashable, Sendable {
    case character
    case punctuation
    case shift
    case backspace
    case symbols
    case inputMode
    case space
    case enter
    case emoji
    case settings

    public var isSpecial: Bool {
        switch self {
        case .character, .punctuation, .space:
            false
        default:
            true
        }
    }
}

public struct KeySpec: Hashable, Sendable, Identifiable {
    public let id: String
    public let label: String
    public let role: KeyRole
    public let widthWeight: CGFloat
    public let shiftedLabel: String?
    public let accessibilityLabel: String

    public init(
        id: String,
        label: String,
        role: KeyRole,
        widthWeight: CGFloat = 1,
        shiftedLabel: String? = nil,
        accessibilityLabel: String? = nil
    ) {
        precondition(!id.isEmpty, "Key id must not be empty")
        precondition(widthWeight > 0, "Key width weight must be positive")

        self.id = id
        self.label = label
        self.role = role
        self.widthWeight = widthWeight
        self.shiftedLabel = shiftedLabel
        self.accessibilityLabel = accessibilityLabel ?? label
    }
}

public struct KeyboardRow: Hashable, Sendable {
    public let keys: [KeySpec]
    public let horizontalInsetUnits: CGFloat

    public init(keys: [KeySpec], horizontalInsetUnits: CGFloat = 0) {
        precondition(!keys.isEmpty, "Keyboard row must contain at least one key")
        precondition(horizontalInsetUnits >= 0, "Row inset must not be negative")

        self.keys = keys
        self.horizontalInsetUnits = horizontalInsetUnits
    }
}

public struct KeyboardLayout: Hashable, Sendable {
    public let id: String
    public let rows: [KeyboardRow]

    public init(id: String, rows: [KeyboardRow]) {
        precondition(!id.isEmpty, "Keyboard layout id must not be empty")
        precondition(!rows.isEmpty, "Keyboard layout must contain at least one row")

        let ids = rows.flatMap(\.keys).map(\.id)
        precondition(Set(ids).count == ids.count, "Key ids must be unique within a layout")

        self.id = id
        self.rows = rows
    }
}

public extension KeyboardLayout {
    static let funputQWERTY = KeyboardLayout(
        id: "funput-qwerty",
        rows: [
            characterRow("qwertyuiop"),
            characterRow("asdfghjkl", inset: 0.5),
            bottomCharacterRow(),
            KeyboardRow(keys: [
                KeySpec(
                    id: "symbols",
                    label: "123",
                    role: .symbols,
                    widthWeight: 1.35,
                    accessibilityLabel: "Số và ký hiệu"
                ),
                KeySpec(
                    id: "input-mode",
                    label: "",
                    role: .inputMode,
                    widthWeight: 1.1,
                    accessibilityLabel: "Bàn phím tiếp theo"
                ),
                KeySpec(
                    id: "space",
                    label: "Tiếng Việt",
                    role: .space,
                    widthWeight: 5.1,
                    accessibilityLabel: "Dấu cách"
                ),
                KeySpec(
                    id: "enter",
                    label: "",
                    role: .enter,
                    widthWeight: 1.7,
                    accessibilityLabel: "Xuống dòng"
                ),
            ]),
        ]
    )
}

private func characterRow(_ characters: String, inset: CGFloat = 0) -> KeyboardRow {
    KeyboardRow(keys: characters.map(characterKey), horizontalInsetUnits: inset)
}

private func bottomCharacterRow() -> KeyboardRow {
    var keys = [
        KeySpec(
            id: "shift",
            label: "",
            role: .shift,
            widthWeight: 1.45,
            accessibilityLabel: "Shift"
        ),
    ]
    keys.append(contentsOf: "zxcvbnm".map(characterKey))
    keys.append(
        KeySpec(
            id: "backspace",
            label: "",
            role: .backspace,
            widthWeight: 1.45,
            accessibilityLabel: "Xóa"
        )
    )
    return KeyboardRow(keys: keys)
}

private func characterKey(_ character: Character) -> KeySpec {
    KeySpec(
        id: "character-\(character)",
        label: String(character),
        role: .character,
        shiftedLabel: String(character).uppercased(),
        accessibilityLabel: String(character)
    )
}
