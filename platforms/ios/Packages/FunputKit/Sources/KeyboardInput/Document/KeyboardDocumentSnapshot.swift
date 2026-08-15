import Foundation

/// The minimum document state needed to validate an active composition.
public struct KeyboardDocumentSnapshot: Equatable, Sendable {
    /// `nil` when the host has not bound a document yet — a state UIKit's nullability
    /// annotation denies but the runtime produces. Two identifier-less snapshots
    /// compare equal, so an unknown document is not mistaken for a changed one.
    public let documentIdentifier: UUID?
    public let contextBeforeInput: String?
    public let hasSelection: Bool

    public init(
        documentIdentifier: UUID?,
        contextBeforeInput: String?,
        hasSelection: Bool
    ) {
        self.documentIdentifier = documentIdentifier
        self.contextBeforeInput = contextBeforeInput
        self.hasSelection = hasSelection
    }
}
