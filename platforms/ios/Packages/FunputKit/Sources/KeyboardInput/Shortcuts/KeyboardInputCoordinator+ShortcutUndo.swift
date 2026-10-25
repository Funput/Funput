#if os(iOS) && canImport(FunputCore)
import FunputEngine

/// A gõ tắt expansion on screen that one Backspace, or a tap on the `↩` chip, can take back.
struct ShortcutUndoState: Equatable {
    /// The word the expansion replaced, as the boundary would have left it: what comes back.
    let shown: String
    /// The expansion with its boundary character: what the document must still end with.
    let written: String
}

extension KeyboardInputCoordinator {
    /// Remember an expansion the engine has just written, so the next Backspace can undo it.
    ///
    /// Asked of the engine rather than guessed from the result: a send at a boundary may
    /// as well be English restore. Only a boundary that sent text ever gets here, so this
    /// costs nothing on an ordinary key. The text is what the document now ends with.
    func noteExpansion(_ result: FunputCompositionResult) {
        guard result.action != .none, let shown = composer.shortcutUndoText else { return }
        shortcuts.undo = ShortcutUndoState(shown: shown, written: result.text)
    }

    /// Take the last expansion back when Backspace is still the very next thing the user does.
    ///
    /// Returns whether Backspace was spent on the undo, in which case the key must **not**
    /// also reach the document: the word and its boundary come back whole, and one more
    /// deletion would eat the space the user never touched.
    ///
    /// Any other Backspace is the user editing the word they refused, so the engine forgets
    /// the refusal too. Erase `vn ` after undoing it, type `vn ` again, and it expands.
    func undoShortcut(builder: inout InputTransactionBuilder) -> Bool {
        guard let undo = shortcuts.undo else {
            if shortcuts.refused {
                composer.dismissShortcutUndo()
                shortcuts.refused = false
            }
            return false
        }
        shortcuts.undo = nil
        // The document is checked *before* the engine is asked, because asking spends the
        // undo: a caret that moved, or a selection, would leave the engine believing it had
        // put the word back while the document still showed the expansion.
        guard usesEngine,
              let snapshot = documentSynchronizer.snapshot,
              !snapshot.hasSelection,
              let context = snapshot.contextBeforeInput,
              context.hasSuffix(undo.written)
        else {
            composer.dismissShortcutUndo()
            return false
        }
        let result = composer.undoShortcut()
        shortcuts.refused = true
        guard result.action != .none,
              let count = KeyboardReplacement.deletionCount(
                scalars: result.deleteCount, context: context
              )
        else { return false }
        builder.deleteBackward(count: count)
        builder.insert(result.text)
        tookBackLastEdit = true
        return true
    }

    /// The user tapped the `↩` chip: the same undo Backspace would make.
    @discardableResult
    public func undoShortcutFromBar(
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        guard case .shortcut = correctionPreview else { return .none }
        return commit(writer: writer, preservesOneShotShift: true) { builder in
            _ = undoShortcut(builder: &builder)
            refreshCorrectionPreview()
        }
    }
}
#endif
