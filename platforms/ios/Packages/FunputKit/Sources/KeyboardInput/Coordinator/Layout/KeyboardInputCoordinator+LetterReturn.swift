#if os(iOS) && canImport(FunputCore)
extension KeyboardInputCoordinator {
    /// Brings back the letters page when a space follows punctuation typed on a symbol
    /// page, so a sentence can go on in Vietnamese without reaching for "ABC".
    ///
    /// Runs inside the space's `commit`, before the space is staged: the shadow still
    /// ends with the punctuation, and the page change lands in the same state diff that
    /// redraws the keyboard and re-arms capitalization for the next word.
    func returnToLettersAfterPunctuationIfNeeded() {
        guard returnsToLettersAfterPunctuation, state.layoutMode != .letters else { return }
        // Read the shadow rather than the proxy, as the smart space does: `commit` has
        // already synchronized it, and it stays correct when the host reports stale context.
        guard let snapshot = documentSynchronizer.snapshot,
              !snapshot.hasSelection,
              LetterPageReturnRule.appliesTo(contextBeforeInput: snapshot.contextBeforeInput)
        else { return }
        updateLayoutMode(.letters)
    }
}
#endif
