import Foundation

/// The minimum document state needed to validate an active composition.
public struct KeyboardDocumentSnapshot: Equatable, Sendable {
    public let documentIdentifier: UUID
    public let contextBeforeInput: String?
    public let hasSelection: Bool

    public init(
        documentIdentifier: UUID,
        contextBeforeInput: String?,
        hasSelection: Bool
    ) {
        self.documentIdentifier = documentIdentifier
        self.contextBeforeInput = contextBeforeInput
        self.hasSelection = hasSelection
    }
}
