import Foundation

public struct KeyAlternate: Hashable, Sendable, Identifiable {
    public let text: String
    public let shiftedText: String
    public let accessibilityLabel: String

    public var id: String { text }

    public init(
        text: String,
        shiftedText: String? = nil,
        accessibilityLabel: String? = nil
    ) {
        precondition(!text.isEmpty, "Alternate text must not be empty")
        self.text = text
        self.shiftedText = shiftedText ?? text.uppercased()
        self.accessibilityLabel = accessibilityLabel ?? text
    }

    public func text(for shiftState: ShiftState) -> String {
        shiftState.isUppercase ? shiftedText : text
    }
}
