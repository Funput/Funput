import Foundation

/// A user-authored text replacement. Its identity survives edits and reloads.
public struct TextShortcut: Codable, Hashable, Sendable, Identifiable {
    public let id: UUID
    public var trigger: String
    public var expansion: String

    public init(id: UUID = UUID(), trigger: String = "", expansion: String = "") {
        self.id = id
        self.trigger = trigger
        self.expansion = expansion
    }

    public var isValid: Bool {
        !trigger.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            && !expansion.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }
}
