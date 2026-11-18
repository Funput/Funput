import InputMethodKit

extension FunputInputController {
    /// Backspace with nothing composing, right after a gõ tắt expansion: put the trigger
    /// back instead of deleting a character. False lets Backspace do its usual work.
    func undoShortcut(_ client: IMKTextInput) -> Bool {
        shortcutUndo.backspace(composer: composer, on: IMKCompositionSurface(client: client))
    }
}
