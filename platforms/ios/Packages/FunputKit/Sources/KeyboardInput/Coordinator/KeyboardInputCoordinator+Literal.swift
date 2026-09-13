#if os(iOS) && canImport(FunputCore)
extension KeyboardInputCoordinator {
    /// Ends any active composition before presenting literal-input UI.
    public func prepareForLiteralInput() {
        clearComposition()
        shiftController.resetTapSequence()
        documentSynchronizer.invalidate()
        resetPersonalSuggestionTracking()
    }

    /// Inserts text exactly as supplied, bypassing Vietnamese composition.
    @discardableResult
    public func insertLiteral(
        _ text: String,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        guard !text.isEmpty else { return .none }
        return commit(
            writer: writer,
            closesEpoch: true,
            preservesOneShotShift: true
        ) { builder in
            clearComposition()
            builder.insert(text)
        }
    }

    /// Deletes one document element while preserving coordinator synchronization.
    @discardableResult
    public func deleteBackward(
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        commit(writer: writer, preservesOneShotShift: true) { builder in
            performDeleteBackward(builder: &builder)
        }
    }

    func performDeleteBackward(builder: inout InputTransactionBuilder) {
        if usesEngine, let last = composer.buffer().last {
            for _ in last.unicodeScalars { composer.backspace() }
            if composer.buffer().isEmpty { finishShortcutWord() }
        } else if !shortcuts.word.isEmpty {
            shortcuts.word.removeLast()
            if shortcuts.word.isEmpty { finishShortcutWord() }
        }
        builder.deleteBackward()
    }

    /// Re-opens the word the caret now sits behind, so the next keystroke retones it
    /// (`chào` ⌫ then `s` gives `cháo`).
    ///
    /// Called only from the keyboard's Backspace key — never from `deleteBackward`,
    /// which is the literal path and must not start composing.
    ///
    /// The document is deliberately left untouched: on iOS the composer buffer mirrors
    /// committed text at the tail of the document, so seeding it with a word that is
    /// already there satisfies the synchronizer's "buffer is a suffix of the context"
    /// invariant on its own. Everything is read from the synchronizer's shadow — no
    /// document read, and immune to a proxy that reports stale text.
    func reopenPreviousWord() {
        guard state.usesVietnameseComposition,
            composer.buffer().isEmpty,
            let snapshot = documentSynchronizer.snapshot,
            !snapshot.hasSelection,
            let word = snapshot.contextBeforeInput?.wordBeforeCursor()
        else { return }
        composer.adopt(word) // refused unless it is a Vietnamese syllable
    }
}
#endif
