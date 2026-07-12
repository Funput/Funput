#if os(iOS) && canImport(FunputCore)
extension KeyboardInputCoordinator {
    /// Ends any active composition before presenting literal-input UI.
    public func prepareForLiteralInput() {
        composer.clear()
        shiftController.resetTapSequence()
        documentSynchronizer.invalidate()
    }

    /// Inserts text exactly as supplied, bypassing Vietnamese composition.
    public func insertLiteral(_ text: String, document: any KeyboardDocument) {
        guard !text.isEmpty else { return }
        synchronizeBeforeInput(document)
        documentSynchronizer.beginMutation()
        composer.clear()
        document.insertText(text)
        finishDocumentMutation(document, preserveOneShotShift: true)
    }

    /// Deletes one document element while preserving coordinator synchronization.
    public func deleteBackward(document: any KeyboardDocument) {
        synchronizeBeforeInput(document)
        documentSynchronizer.beginMutation()
        performDeleteBackward(document: document)
        finishDocumentMutation(document, preserveOneShotShift: true)
    }

    func performDeleteBackward(document: any KeyboardDocument) {
        if state.usesVietnameseComposition {
            composer.backspace()
        }
        document.deleteBackward()
    }
}
#endif
