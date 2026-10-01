#if os(iOS) && canImport(FunputCore)
import FunputEngine

extension KeyboardInputCoordinator {
    /// Take back the correction applied on the previous keystroke, when Backspace is
    /// still the very next thing the user does.
    ///
    /// Returns whether Backspace was spent on the undo. When it was, the key must
    /// **not** also be passed through to the document: the engine has already said
    /// exactly what to delete and what to put back, and deleting one more character
    /// on top of that would eat the space the user never touched.
    ///
    /// The word restored is the one they typed, not the one that was corrected away,
    /// and the dictionary is told to keep it, so it is never corrected again.
    func undoCorrection(builder: inout InputTransactionBuilder) -> Bool {
        // The context is checked *before* the engine is asked, because asking spends
        // the undo: a failure afterwards would leave the engine believing it had put
        // the word back while the document still showed the correction.
        guard usesEngine, composer.hasCorrectionUndo,
              let context = documentSynchronizer.snapshot?.contextBeforeInput,
              !context.isEmpty
        else { return false }
        let result = composer.backspace()
        guard result.action != .none,
              let count = KeyboardReplacement.deletionCount(
                scalars: result.deleteCount, context: context
              )
        else {
            // The document is not where the engine thinks it is, so fall back to the
            // plain Backspace the caller would otherwise have made.
            return false
        }
        builder.deleteBackward(count: count)
        builder.insert(result.text)
        undidCorrection = true
        // What comes back is the word as typed plus the boundary after it. The engine
        // only suppresses the word once; the dictionary makes it permanent.
        correctionDictionary?.keep(String(result.text.dropLast()))
        return true
    }
}
#endif
