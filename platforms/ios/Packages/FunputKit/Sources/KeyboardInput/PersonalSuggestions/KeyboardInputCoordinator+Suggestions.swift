#if os(iOS) && canImport(FunputCore)
import KeyboardLayout

public extension KeyboardInputCoordinator {
    func takePersonalSuggestionUpdate() -> KeyboardSuggestionInputUpdate {
        guard tracksPersonalSuggestions else {
            suggestionTracker.reset()
            return .empty
        }
        return suggestionTracker.consume()
    }

    @discardableResult
    func acceptSuggestion(
        _ suggestion: String,
        replacing prefix: String,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects? {
        guard tracksPersonalSuggestions,
              !suggestion.isEmpty,
              suggestionTracker.prefix == prefix else { return nil }
        _ = synchronizeDocument(writer, event: .textChanged)
        guard suggestionTracker.prefix == prefix else { return nil }
        let snapshot = writer.snapshot
        guard snapshot.documentIdentifier == documentSynchronizer.snapshot?.documentIdentifier,
              !snapshot.hasSelection,
              snapshot.contextBeforeInput.map({ $0.hasSuffix(prefix) }) ?? true else {
            suggestionTracker.reset()
            return nil
        }

        return commit(
            writer: writer,
            closesEpoch: true,
            preservesOneShotShift: false
        ) { builder in
            composer.clear()
            builder.deleteBackward(count: prefix.count)
            builder.insert(suggestion + " ")
        }
    }
}

extension KeyboardInputCoordinator {
    var tracksPersonalSuggestions: Bool {
        suggestionTrackingActive
    }

    func resetPersonalSuggestionTracking() {
        suggestionTracker.reset()
    }
}
#endif
