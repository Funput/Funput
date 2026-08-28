#if os(iOS) && canImport(FunputCore)
extension KeyboardInputCoordinator {
    /// Moves the caret for the spacebar trackpad: `columns` characters sideways and
    /// `lines` logical lines vertically, in one document transaction.
    ///
    /// Composition cannot survive the caret leaving the buffer it mirrors, so every step
    /// ends it. Deliberately not routed through ``commit(writer:closesEpoch:preservesOneShotShift:reopensPreviousWord:build:)``:
    /// that path re-resolves capitalization from a shadow this mutation has just dropped.
    /// Shift is resolved instead from the host's own selection callback.
    @discardableResult
    public func moveCaret(
        columns: Int,
        lines: Int,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        let resolution = resolveCaretStep(columns: columns, lines: lines, writer: writer)
        guard resolution.offset != 0 else { return .none }
        prepareForLiteralInput()
        let transaction = InputTransaction(
            sequence: nextTransactionSequence,
            mutations: [.moveCursor(offset: resolution.offset)],
            resultingState: state
        )
        nextTransactionSequence += 1
        caretPan = resolution.column.map {
            (desiredColumn: $0, sequence: nextTransactionSequence)
        }
        writer.apply(transaction)
        return .init(suggestionsChanged: true)
    }

    private func resolveCaretStep(
        columns: Int,
        lines: Int,
        writer: any KeyboardDocumentWriting
    ) -> CaretLineGeometry.Resolution {
        // The horizontal-only step is the common one and stays free of proxy reads: a
        // character offset needs no knowledge of where the lines are.
        guard lines != 0 else {
            return CaretLineGeometry.Resolution(offset: columns, column: nil)
        }
        // Reused only when the transaction right before this one was this same pan's own
        // move. Anything typed in between advances the sequence and drops the memory, so
        // no reset path can be forgotten.
        let remembered = caretPan.flatMap {
            $0.sequence == nextTransactionSequence ? $0.desiredColumn : nil
        }
        return CaretLineGeometry(writer.caretContext)
            .resolve(columns: columns, lines: lines, desiredColumn: remembered)
    }
}
#endif
