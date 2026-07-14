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

@MainActor
public protocol KeyboardDocument {
    var documentIdentifier: UUID { get }
    /// The text before the caret. On iOS this is a cross-process read, so the
    /// hot typing path fetches only the individual fields it needs.
    var contextBeforeInput: String? { get }
    var hasSelection: Bool { get }
    /// A transient snapshot. Callers must not persist or log its text context.
    var snapshot: KeyboardDocumentSnapshot { get }

    func insertText(_ text: String)
    func deleteBackward()
}

public extension KeyboardDocument {
    /// A convenience snapshot for callers outside the latency-sensitive input path.
    var snapshot: KeyboardDocumentSnapshot {
        KeyboardDocumentSnapshot(
            documentIdentifier: documentIdentifier,
            contextBeforeInput: contextBeforeInput,
            hasSelection: hasSelection
        )
    }
}
