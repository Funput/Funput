#if os(iOS) && canImport(FunputCore)
extension KeyboardInputCoordinator {
    /// Moves the caret by whole characters for the spacebar trackpad.
    ///
    /// Composition cannot survive the caret leaving the buffer it mirrors, so every step
    /// ends it. Deliberately not routed through ``commit(writer:closesEpoch:preservesOneShotShift:reopensPreviousWord:build:)``:
    /// that path re-resolves capitalization from a shadow this mutation has just dropped.
    /// Shift is resolved instead from the host's own selection callback.
    @discardableResult
    public func moveCursor(
        by offset: Int,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        guard offset != 0 else { return .none }
        prepareForLiteralInput()
        let transaction = InputTransaction(
            sequence: nextTransactionSequence,
            mutations: [.moveCursor(offset: offset)],
            resultingState: state
        )
        nextTransactionSequence += 1
        writer.apply(transaction)
        return .init(suggestionsChanged: true)
    }
}
#endif
