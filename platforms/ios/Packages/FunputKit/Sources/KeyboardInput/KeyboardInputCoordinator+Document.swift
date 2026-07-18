#if os(iOS) && canImport(FunputCore)
import Foundation
import KeyboardLayout

extension KeyboardInputCoordinator {
    /// Reconciles composer and Shift state with a UIKit document lifecycle event.
    public func synchronizeDocument(
        _ document: any KeyboardDocument,
        event: KeyboardDocumentEvent
    ) {
        guard !documentSynchronizer.isApplyingMutation else { return }
        let identifier = document.documentIdentifier
        let context = document.contextBeforeInput
        if event == .textChanged,
           documentSynchronizer.consumeAuthoredTextChange(
               documentIdentifier: identifier,
               contextBeforeInput: context
           ) {
            return
        }
        let snapshot = KeyboardDocumentSnapshot(
            documentIdentifier: identifier,
            contextBeforeInput: context,
            hasSelection: document.hasSelection
        )
        reconcile(snapshot, event: event)
    }

    func synchronizeBeforeInput(_ document: any KeyboardDocument) {
        // Lifecycle callbacks keep the shadow document synchronized. Only the
        // first key after activation/invalidation needs a proxy read.
        guard documentSynchronizer.snapshot == nil else { return }
        synchronizeDocument(document, event: .textChanged)
    }

    func finishDocumentMutation(
        preserveOneShotShift: Bool
    ) {
        if let mutation = documentSynchronizer.finishMutation(), mutation.changed {
            synchronizeCapitalization(
                with: mutation.snapshot,
                preserveCapsLock: true,
                preserveOneShotShift: preserveOneShotShift
            )
        }
    }

    func insertDocumentText(_ text: String, document: any KeyboardDocument) {
        let signpostID = KeyboardInputSignposts.begin("ProxyInsert")
        document.insertText(text)
        KeyboardInputSignposts.end("ProxyInsert", signpostID)
        documentSynchronizer.recordInsertion(text)
    }

    func deleteDocumentBackward(_ document: any KeyboardDocument) {
        let signpostID = KeyboardInputSignposts.begin("ProxyDelete")
        document.deleteBackward()
        KeyboardInputSignposts.end("ProxyDelete", signpostID)
        documentSynchronizer.recordDeletion()
    }

    private func reconcile(
        _ snapshot: KeyboardDocumentSnapshot,
        event: KeyboardDocumentEvent
    ) {
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
