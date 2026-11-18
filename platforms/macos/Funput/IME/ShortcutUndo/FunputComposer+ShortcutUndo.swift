import Foundation

/// The engine's one-tap undo of a gõ tắt expansion. Every call is local to this process:
/// nothing here reaches the focused app.
extension FunputComposer {
    /// Whether Backspace could still take the last expansion back.
    var hasShortcutUndo: Bool {
        funput_engine_shortcut_undo_text(handle, nil, nil)
    }

    /// What the last expansion wrote, boundary character included: exactly the text the
    /// undo deletes. Nil once nothing is armed.
    func shortcutUndoWritten() -> String? {
        let (armed, written) = Self.receivingText {
            funput_engine_shortcut_undo_written(handle, Self.receiveText, $0)
        }
        return armed ? written : nil
    }

    /// Spend the undo and return the text that replaces the expansion: the word as it was
    /// shown before it expanded, plus the same boundary character. The engine then refuses
    /// that trigger once. Nil when nothing was armed.
    func undoShortcut() -> String? {
        let (result, restored) = Self.receivingText {
            funput_engine_undo_shortcut(handle, Self.receiveText, $0)
        }
        return result.action == ACTION_SEND ? restored : nil
    }

    /// Drop the undo, and the refusal it would leave: the expansion is no longer known to
    /// sit before the caret.
    func dismissShortcutUndo() {
        funput_engine_dismiss_shortcut_undo(handle)
    }
}
