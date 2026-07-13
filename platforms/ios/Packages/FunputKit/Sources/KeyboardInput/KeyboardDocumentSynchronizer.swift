import Foundation

/// UIKit document lifecycle event observed by the keyboard extension.
public enum KeyboardDocumentEvent: Equatable, Sendable {
    case activated
    case textChanged
    case selectionChanged
}

@MainActor
struct KeyboardDocumentSynchronizer {
    private static let shadowContextLimit = 128
    private static let authoredContextLimit = 256

    private(set) var snapshot: KeyboardDocumentSnapshot?
    private(set) var isApplyingMutation = false
    private var snapshotBeforeMutation: KeyboardDocumentSnapshot?
    private var authoredContexts: [String?] = []
    private var nextAuthoredContextIndex = 0

    var pendingAuthoredContextCount: Int { authoredContexts.count }

    mutating func beginMutation() {
        snapshotBeforeMutation = snapshot
        isApplyingMutation = true
    }

    mutating func finishMutation() -> (snapshot: KeyboardDocumentSnapshot, changed: Bool)? {
        isApplyingMutation = false
        defer { snapshotBeforeMutation = nil }
        guard let snapshot else { return nil }
        return (snapshot, snapshotBeforeMutation != snapshot)
    }

    mutating func recordInsertion(_ text: String) {
        guard !text.isEmpty, let current = snapshot else { return }
        let context = current.contextBeforeInput.map {
            String(($0 + text).suffix(Self.shadowContextLimit))
        }
        updateAuthoredSnapshot(from: current, contextBeforeInput: context)
    }

    mutating func recordDeletion() {
        guard let current = snapshot else { return }
        var context = current.contextBeforeInput
        if !current.hasSelection, context?.isEmpty == false {
            context?.removeLast()
        }
        updateAuthoredSnapshot(from: current, contextBeforeInput: context)
    }

    /// Returns true for callbacks produced by mutations already reflected in the
    /// local shadow document. Intermediate delete/insert callbacks are ignored
    /// until the host reaches the final shadow context.
    mutating func consumeAuthoredTextChange(
        documentIdentifier: UUID,
        contextBeforeInput: String?
    ) -> Bool {
        guard let snapshot,
              snapshot.documentIdentifier == documentIdentifier else { return false }
        if contextsMatch(snapshot.contextBeforeInput, contextBeforeInput) {
            clearAuthoredContexts()
            return true
        }
        return authoredContexts.contains {
            contextsMatch($0, contextBeforeInput)
        }
    }

    mutating func accept(_ snapshot: KeyboardDocumentSnapshot) {
        self.snapshot = snapshot
        clearAuthoredContexts()
    }

    mutating func invalidate() {
        snapshot = nil
        isApplyingMutation = false
        snapshotBeforeMutation = nil
        clearAuthoredContexts()
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

    private mutating func updateAuthoredSnapshot(
        from current: KeyboardDocumentSnapshot,
        contextBeforeInput: String?
    ) {
        snapshot = KeyboardDocumentSnapshot(
            documentIdentifier: current.documentIdentifier,
            contextBeforeInput: contextBeforeInput,
            hasSelection: false
        )
        appendAuthoredContext(contextBeforeInput)
    }

    private mutating func appendAuthoredContext(_ context: String?) {
        if authoredContexts.count < Self.authoredContextLimit {
            authoredContexts.append(context)
            return
        }
        authoredContexts[nextAuthoredContextIndex] = context
        nextAuthoredContextIndex = (nextAuthoredContextIndex + 1)
            % Self.authoredContextLimit
    }

    private mutating func clearAuthoredContexts() {
        authoredContexts.removeAll(keepingCapacity: true)
        nextAuthoredContextIndex = 0
    }

    private func contextsMatch(_ expected: String?, _ actual: String?) -> Bool {
        switch (expected, actual) {
        case (nil, nil):
            true
        case let (expected?, actual?):
            expected == actual
                || (!expected.isEmpty && !actual.isEmpty
                    && (expected.hasSuffix(actual) || actual.hasSuffix(expected)))
        default:
            false
        }
    }
}
