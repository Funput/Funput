import Foundation

@MainActor
extension KeyboardDocumentSynchronizer {
    func contextContainsBuffer(_ context: String?, _ buffer: String) -> Bool {
        guard !buffer.isEmpty else { return true }
        return normalize(context).hasSuffix(buffer)
    }

    mutating func updateAuthoredSnapshot(
        from current: KeyboardDocumentSnapshot,
        contextBeforeInput: String
    ) {
        snapshot = KeyboardDocumentSnapshot(
            documentIdentifier: current.documentIdentifier,
            contextBeforeInput: contextBeforeInput,
            hasSelection: false
        )
        appendAuthoredContext(contextBeforeInput)
    }

    mutating func appendAuthoredContext(_ context: String?) {
        let context = normalize(context)
        if currentEpoch.contexts.last == context { return }
        currentEpoch.contexts.append(context)
        if currentEpoch.contexts.count > Self.epochContextLimit {
            currentEpoch.contexts.removeFirst(16)
        }
        trimPreviousEpoch()
    }

    mutating func closeCurrentEpoch() {
        let retained = Array(currentEpoch.contexts.suffix(Self.retainedPreviousContextLimit))
        previousEpoch = EchoEpoch(generation: currentEpoch.generation, contexts: retained)
        currentEpoch = EchoEpoch(generation: nextGeneration)
        nextGeneration &+= 1
    }

    mutating func trimPreviousEpoch() {
        let available = max(0, Self.epochContextLimit - currentEpoch.contexts.count)
        let keep = min(Self.retainedPreviousContextLimit, available)
        guard var previous = previousEpoch, previous.contexts.count > keep else { return }
        previous.contexts = Array(previous.contexts.suffix(keep))
        previousEpoch = previous
    }

    mutating func resetEchoLedger() {
        previousEpoch = nil
        currentEpoch = EchoEpoch(generation: nextGeneration)
        nextGeneration &+= 1
    }

    func normalized(_ snapshot: KeyboardDocumentSnapshot) -> KeyboardDocumentSnapshot {
        KeyboardDocumentSnapshot(
            documentIdentifier: snapshot.documentIdentifier,
            contextBeforeInput: normalize(snapshot.contextBeforeInput),
            hasSelection: snapshot.hasSelection
        )
    }

    func normalize(_ context: String?) -> String {
        String((context ?? "").suffix(Self.shadowContextLimit))
    }
}
