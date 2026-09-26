#if os(iOS) && canImport(FunputCore)
import KeyboardLayout

extension KeyboardInputCoordinator {
    /// Types a cell chosen from a key's hold palette. The cell behaves like the key it was
    /// offered on: a letter's variant joins the syllable being composed, while a symbol from
    /// a punctuation key is typed exactly as if that symbol had its own key.
    @discardableResult
    public func handleAlternate(
        _ alternate: KeyAlternate,
        from key: KeySpec,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        switch key.role {
        case .character: commitCharacterAlternate(alternate, writer: writer)
        case .punctuation: commitPunctuationAlternate(alternate, writer: writer)
        default: .none
        }
    }

    private func commitCharacterAlternate(
        _ alternate: KeyAlternate,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        commit(writer: writer, preservesOneShotShift: false) { builder in
            input(alternate.text(for: state.shiftState), builder: &builder)
            consumeOneShotShift()
        }
    }

    /// Mirrors a punctuation tap: it closes the composition epoch, breaks a double-space
    /// run and leaves a one-shot Shift armed for the word that follows.
    private func commitPunctuationAlternate(
        _ alternate: KeyAlternate,
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        commit(
            writer: writer,
            closesEpoch: closesCompositionEpoch(role: .punctuation, text: alternate.text),
            preservesOneShotShift: true
        ) { builder in
            spaceTapTracker.reset()
            input(alternate.text, builder: &builder)
        }
    }
}
#endif
