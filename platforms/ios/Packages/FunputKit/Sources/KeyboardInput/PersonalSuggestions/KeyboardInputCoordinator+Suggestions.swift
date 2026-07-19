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
        document: any KeyboardDocument
    ) -> Bool {
        guard tracksPersonalSuggestions,
              !suggestion.isEmpty,
              suggestionTracker.prefix == prefix else { return false }
        synchronizeDocument(document, event: .textChanged)
        guard suggestionTracker.prefix == prefix else { return false }
        let snapshot = document.snapshot
        guard snapshot.documentIdentifier == documentSynchronizer.snapshot?.documentIdentifier,
              !snapshot.hasSelection,
              snapshot.contextBeforeInput.map({ $0.hasSuffix(prefix) }) ?? true else {
            suggestionTracker.reset()
            return false
        }

        documentSynchronizer.beginMutation(closesEpoch: true)
        composer.clear()
        for _ in prefix { deleteDocumentBackward(document) }
        insertDocumentText(suggestion + " ", document: document)
        finishDocumentMutation(preserveOneShotShift: false)
        return true
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
