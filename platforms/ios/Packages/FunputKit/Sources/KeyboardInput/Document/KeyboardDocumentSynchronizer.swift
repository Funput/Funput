import Foundation

/// UIKit document lifecycle event observed by the keyboard extension.
public enum KeyboardDocumentEvent: Equatable, Sendable {
    case activated
    case textChanged
    case selectionChanged
}

/// Maintains a bounded local shadow and a generation-scoped ledger of contexts
/// authored by Funput. Exact normalized matching acknowledges delayed UIKit
/// echoes without allowing arbitrary old suffixes to keep composition alive.
@MainActor
struct KeyboardDocumentSynchronizer {
    static let shadowContextLimit = 128
    static let epochContextLimit = 64
    static let retainedPreviousContextLimit = 32

    var clock: () -> TimeInterval = { ProcessInfo.processInfo.systemUptime }
    var snapshot: KeyboardDocumentSnapshot?
    private(set) var isApplyingMutation = false
    var snapshotBeforeMutation: KeyboardDocumentSnapshot?
    var currentEpoch = KeyboardEchoEpoch(generation: 0)
    var previousEpoch: KeyboardEchoEpoch?
    var nextGeneration: UInt64 = 1
    var closesEpochAfterMutation = false
    var stagedAuthoredContexts: [String] = []

    var pendingAuthoredContextCount: Int {
        currentEpoch.contexts.count + (previousEpoch?.contexts.count ?? 0)
    }

    mutating func beginMutation(closesEpoch: Bool = false) {
        snapshotBeforeMutation = snapshot
        closesEpochAfterMutation = closesEpoch
        if let snapshot {
            stagedAuthoredContexts = [normalize(snapshot.contextBeforeInput)]
        } else {
            stagedAuthoredContexts = []
        }
        isApplyingMutation = true
    }

    mutating func finishMutation() -> (snapshot: KeyboardDocumentSnapshot, changed: Bool)? {
        isApplyingMutation = false
        for context in stagedAuthoredContexts {
            appendAuthoredContext(context)
        }
        defer {
            snapshotBeforeMutation = nil
            stagedAuthoredContexts = []
            if closesEpochAfterMutation { closeCurrentEpoch() }
            closesEpochAfterMutation = false
        }
        guard let snapshot else { return nil }
        return (snapshot, snapshotBeforeMutation != snapshot)
    }

    mutating func recordInsertion(_ text: String) {
        guard !text.isEmpty, let current = snapshot else { return }
        let context = normalize((current.contextBeforeInput ?? "") + text)
        updateAuthoredSnapshot(from: current, contextBeforeInput: context)
    }

    mutating func recordDeletion() {
        guard let current = snapshot else { return }
        var context = normalize(current.contextBeforeInput)
        if !current.hasSelection, !context.isEmpty { context.removeLast() }
        updateAuthoredSnapshot(from: current, contextBeforeInput: context)
    }

    mutating func stage(_ mutations: [DocumentMutation]) {
        for mutation in mutations {
            switch mutation {
            case let .deleteBackward(count):
                for _ in 0..<count { recordDeletion() }
            case let .insert(text):
                recordInsertion(text)
            }
        }
    }

    mutating func consumeAuthoredTextChange(
        documentIdentifier: UUID?,
        contextBeforeInput: String?
    ) -> Bool {
        guard let snapshot,
              snapshot.documentIdentifier == documentIdentifier else { return false }
        let context = normalize(contextBeforeInput)
        if normalize(snapshot.contextBeforeInput) == context { return true }
        let horizon = clock() - KeyboardEchoEpoch.echoLifetime
        return currentEpoch.acknowledges(context, notBefore: horizon)
            || previousEpoch?.acknowledges(context, notBefore: horizon) == true
    }

    mutating func accept(
        _ snapshot: KeyboardDocumentSnapshot,
        preservingAuthoredEchoes: Bool = false
    ) {
        self.snapshot = normalized(snapshot)
        guard !preservingAuthoredEchoes else { return }
        resetEchoLedger()
    }

    mutating func invalidate() {
        snapshot = nil
        isApplyingMutation = false
        snapshotBeforeMutation = nil
        closesEpochAfterMutation = false
        stagedAuthoredContexts = []
        resetEchoLedger()
    }

    func requiresCompositionReset(
        for current: KeyboardDocumentSnapshot,
        composerBuffer: String
    ) -> Bool {
        let current = normalized(current)
        guard let previous = snapshot else {
            return !contextContainsBuffer(current.contextBeforeInput, composerBuffer)
        }
        if previous.documentIdentifier != current.documentIdentifier { return true }
        if current.hasSelection, !composerBuffer.isEmpty { return true }
        guard !composerBuffer.isEmpty else { return false }
        if normalize(previous.contextBeforeInput) != normalize(current.contextBeforeInput) {
            return true
        }
        return !contextContainsBuffer(current.contextBeforeInput, composerBuffer)
    }

}
