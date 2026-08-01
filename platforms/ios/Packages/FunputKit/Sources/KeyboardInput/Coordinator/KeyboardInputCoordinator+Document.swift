#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardLayout

extension KeyboardInputCoordinator {
    @discardableResult
    public func synchronizeDocument(
        _ writer: any KeyboardDocumentWriting,
        event: KeyboardDocumentEvent
    ) -> KeyboardPostCommitEffects {
        guard !documentSynchronizer.isApplyingMutation else { return .none }
        let previousState = state
        let snapshot = writer.snapshot
        if event.acknowledgesAuthoredEcho,
           !snapshot.hasSelection,
           documentSynchronizer.consumeAuthoredTextChange(
               documentIdentifier: snapshot.documentIdentifier,
               contextBeforeInput: snapshot.contextBeforeInput
           ) {
            return .none
        }
        reconcile(snapshot, event: event)
        return .init(
            presentationChanged: state != previousState,
            suggestionsChanged: true
        )
    }

    func synchronizeBeforeInput(_ writer: any KeyboardDocumentWriting) {
        guard documentSynchronizer.snapshot == nil else { return }
        _ = synchronizeDocument(writer, event: .textChanged)
    }

    func commit(
        writer: any KeyboardDocumentWriting,
        closesEpoch: Bool = false,
        preservesOneShotShift: Bool,
        reopensPreviousWord: Bool = false,
        build: (inout InputTransactionBuilder) -> Void
    ) -> KeyboardPostCommitEffects {
        synchronizeBeforeInput(writer)
        let previousState = state
        documentSynchronizer.beginMutation(closesEpoch: closesEpoch)
        var builder = InputTransactionBuilder()
        build(&builder)
        documentSynchronizer.stage(builder.mutations)
        // Only now does the shadow describe the document after this key. Re-opening from
        // inside `build` would read the text as it was before the deletion.
        //
        // The selection check has to look at the snapshot from before the mutation: staging a
        // deletion always clears `hasSelection`, and a deletion that replaced a selection left
        // the shadow holding text the document no longer contains.
        if reopensPreviousWord,
           documentSynchronizer.snapshotBeforeMutation?.hasSelection != true {
            reopenPreviousWord()
        }
        if let snapshot = documentSynchronizer.snapshot,
           snapshot != documentSynchronizer.snapshotBeforeMutation {
            synchronizeCapitalization(
                with: snapshot,
                preserveCapsLock: true,
                preserveOneShotShift: preservesOneShotShift
            )
        }
        guard !builder.isEmpty else {
            _ = documentSynchronizer.finishMutation()
            return .init(presentationChanged: state != previousState)
        }
        let transaction = InputTransaction(
            sequence: nextTransactionSequence,
            mutations: builder.mutations,
            resultingState: state
        )
        nextTransactionSequence += 1
        writer.apply(transaction)
        _ = documentSynchronizer.finishMutation()
        if tracksPersonalSuggestions {
            suggestionTracker.apply(transaction.mutations)
        }
        return .init(
            presentationChanged: state != previousState,
            suggestionsChanged: true
        )
    }

    func reconcile(
        _ snapshot: KeyboardDocumentSnapshot,
        event: KeyboardDocumentEvent
    ) {
        suggestionTracker.reset()
        let previous = documentSynchronizer.snapshot
        let documentChanged = previous?.documentIdentifier != snapshot.documentIdentifier
        let snapshotChanged = previous != snapshot
        let mustReset = event == .activated || documentSynchronizer.requiresCompositionReset(
            for: snapshot,
            composerBuffer: composer.buffer()
        )
        if mustReset {
            resetCompositionState()
        }
        documentSynchronizer.accept(
            snapshot,
            preservingAuthoredEchoes: event != .activated
        )
        if event == .activated || snapshotChanged {
            synchronizeCapitalization(
                with: snapshot,
                preserveCapsLock: event != .activated && !documentChanged
            )
        }
    }

    private func resetCompositionState() {
        composer.clear()
        shiftController.resetTapSequence()
    }

    private func synchronizeCapitalization(
        with snapshot: KeyboardDocumentSnapshot,
        preserveCapsLock: Bool,
        preserveOneShotShift: Bool = false
    ) {
        guard !snapshot.hasSelection else { return }
        if preserveCapsLock, state.shiftState == .capsLocked { return }
        if preserveOneShotShift, state.shiftState == .uppercase { return }
        guard let uppercase = KeyboardCapitalizationResolver.shouldUppercase(
            mode: state.autocapitalization,
            contextBeforeInput: snapshot.contextBeforeInput
        ) else { return }
        replaceState(shiftState: uppercase ? .uppercase : .lowercase)
    }
}
#endif
