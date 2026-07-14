import Foundation

public struct KeySpec: Hashable, Sendable, Identifiable {
    public let id: String
    public let label: String
    public let role: KeyRole
    public let widthWeight: CGFloat
    public let shiftedLabel: String?
    public let secondaryLabel: String?
    public let accessibilityLabel: String
    public let horizontalSwipeAction: KeySwipeAction?

    public init(
        id: String,
        label: String,
        role: KeyRole,
        widthWeight: CGFloat = 1,
        shiftedLabel: String? = nil,
        secondaryLabel: String? = nil,
        accessibilityLabel: String? = nil,
        horizontalSwipeAction: KeySwipeAction? = nil
    ) {
        precondition(!id.isEmpty, "Key id must not be empty")
        precondition(widthWeight > 0, "Key width weight must be positive")

        self.id = id
        self.label = label
        self.role = role
        self.widthWeight = widthWeight
        self.shiftedLabel = shiftedLabel
        self.secondaryLabel = secondaryLabel
        self.accessibilityLabel = accessibilityLabel ?? label
        self.horizontalSwipeAction = horizontalSwipeAction
    }
}
