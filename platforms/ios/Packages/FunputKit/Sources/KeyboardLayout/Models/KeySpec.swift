import Foundation

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
