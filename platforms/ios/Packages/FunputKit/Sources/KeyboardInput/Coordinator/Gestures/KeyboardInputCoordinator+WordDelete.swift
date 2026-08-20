#if os(iOS) && canImport(FunputCore)
extension KeyboardInputCoordinator {
    /// Deletes the word behind the caret, plus any spaces between it and the caret.
    ///
    /// Falls back to a single backspace whenever the span cannot be measured — a
    /// selection is pending, or the host reports no context — because deleting a guessed
    /// number of characters is far worse than deleting one too few.
    @discardableResult
    public func deleteWordBackward(
        writer: any KeyboardDocumentWriting
    ) -> KeyboardPostCommitEffects {
        synchronizeBeforeInput(writer)
        guard let snapshot = documentSynchronizer.snapshot,
              !snapshot.hasSelection,
              let count = KeyboardWordDeletion.spanBeforeCursor(snapshot.contextBeforeInput)
        else { return deleteBackward(writer: writer) }
        return commit(
            writer: writer,
            closesEpoch: true,
            preservesOneShotShift: true
        ) { builder in
            // Literal deletion: the span covers committed text, so the composer is ended
            // rather than walked back character by character.
            composer.clear()
            builder.deleteBackward(count: count)
        }
    }
}

enum KeyboardWordDeletion {
    /// The number of characters a word-delete should remove, or nil when there is nothing
    /// measurable behind the caret.
    ///
    /// Trailing whitespace goes first, so a swipe after `"xin chao "` takes the word too
    /// rather than only the space the caret visually sits behind. What follows is either a
    /// word or a run of punctuation — `"a!!!"` loses all three marks, not one.
    static func spanBeforeCursor(_ context: String?) -> Int? {
        guard let context, !context.isEmpty else { return nil }
        let spaces = context.reversed().prefix { $0.isWhitespace && !$0.isNewline }.count
        let trimmed = String(context.dropLast(spaces))
        guard let last = trimmed.last else { return spaces > 0 ? spaces : nil }
        let run = last.isCompositionBoundary
            ? trimmed.reversed().prefix { $0.isCompositionBoundary && !$0.isWhitespace }.count
            : (trimmed.wordBeforeCursor()?.count ?? 0)
        let span = spaces + run
        return span > 0 ? span : nil
    }
}

#endif
