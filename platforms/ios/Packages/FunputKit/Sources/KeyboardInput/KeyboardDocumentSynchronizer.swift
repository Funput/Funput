/// UIKit document lifecycle event observed by the keyboard extension.
public enum KeyboardDocumentEvent: Equatable, Sendable {
    case activated
    case textChanged
    case selectionChanged
}

@MainActor
struct KeyboardDocumentSynchronizer {
    private(set) var snapshot: KeyboardDocumentSnapshot?
    private(set) var isApplyingMutation = false

    mutating func beginMutation() {
        isApplyingMutation = true
    }

    mutating func finishMutation(with snapshot: KeyboardDocumentSnapshot) {
        self.snapshot = snapshot
        isApplyingMutation = false
    }

    mutating func accept(_ snapshot: KeyboardDocumentSnapshot) {
        self.snapshot = snapshot
    }

    mutating func invalidate() {
        snapshot = nil
        isApplyingMutation = false
    }

    func requiresCompositionReset(
        for current: KeyboardDocumentSnapshot,
        composerBuffer: String
    ) -> Bool {
        guard let previous = snapshot else {
            return !contextContainsBuffer(current.contextBeforeInput, composerBuffer)
        }
        if previous.documentIdentifier != current.documentIdentifier {
            return true
        }
        if current.hasSelection, !composerBuffer.isEmpty {
            return true
        }
        guard !composerBuffer.isEmpty else { return false }
        if let oldContext = previous.contextBeforeInput,
           let newContext = current.contextBeforeInput,
           oldContext != newContext {
            return true
        }
        return !contextContainsBuffer(current.contextBeforeInput, composerBuffer)
    }

    private func contextContainsBuffer(_ context: String?, _ buffer: String) -> Bool {
        guard !buffer.isEmpty, let context else { return true }
        return context.hasSuffix(buffer)
    }
}
