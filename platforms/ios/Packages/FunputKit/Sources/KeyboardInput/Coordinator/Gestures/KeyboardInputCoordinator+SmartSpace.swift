#if os(iOS) && canImport(FunputCore)
extension KeyboardInputCoordinator {
    /// Turns the second of two quick spaces into `". "`, the way the system keyboard does.
    ///
    /// Staged into the same transaction as an ordinary space so the document shadow, the
    /// echo ledger and the suggestion tracker all see one coherent mutation. Returns
    /// whether the substitution was made; the caller inserts a plain space otherwise.
    func applySmartSpace(builder: inout InputTransactionBuilder) -> Bool {
        guard smartGesturesEnabled else { return false }
        guard spaceTapTracker.registerSpace() else { return false }
        // Read the shadow rather than the proxy: `commit` has already synchronized it, and
        // it is the copy that stays correct when the host reports stale context.
        guard let snapshot = documentSynchronizer.snapshot,
              !snapshot.hasSelection,
              SentencePunctuationRule.appliesTo(
                  contextBeforeInput: snapshot.contextBeforeInput
              )
        else { return false }
        // The trailing space is literal text, never part of a syllable, so the composer is
        // cleared instead of being asked to delete through it.
        composer.clear()
        builder.deleteBackward()
        builder.insert(". ")
        return true
    }
}
#endif
