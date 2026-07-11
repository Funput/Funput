import Foundation

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
