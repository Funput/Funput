#if os(iOS) && canImport(FunputCore)
import KeyboardLayout

extension KeyboardInputCoordinator {
    /// Reconciles composer and Shift state with a UIKit document lifecycle event.
    public func synchronizeDocument(
        _ document: any KeyboardDocument,
        event: KeyboardDocumentEvent
    ) {
        guard !documentSynchronizer.isApplyingMutation else { return }
        let snapshot = document.snapshot
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
        documentSynchronizer.accept(snapshot)

        if event == .activated || snapshotChanged {
            synchronizeCapitalization(
                with: snapshot,
                preserveCapsLock: event != .activated && !documentChanged
            )
        }
    }

    func synchronizeBeforeInput(_ document: any KeyboardDocument) {
        synchronizeDocument(document, event: .textChanged)
    }

    func finishDocumentMutation(
        _ document: any KeyboardDocument,
        preserveOneShotShift: Bool
    ) {
        let snapshot = document.snapshot
        let changed = documentSynchronizer.snapshot != snapshot
        documentSynchronizer.finishMutation(with: snapshot)
        if changed {
            synchronizeCapitalization(
                with: snapshot,
                preserveCapsLock: true,
                preserveOneShotShift: preserveOneShotShift
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
