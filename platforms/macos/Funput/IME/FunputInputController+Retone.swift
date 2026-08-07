import AppKit
import InputMethodKit

extension FunputInputController {
    /// Backspace with nothing composing: give the word the caret is about to land on back
    /// to the engine, so the next keystroke retones it instead of typing a literal letter
    /// (`chào` + Space + ⌫ + `s` → `cháo`) — the behaviour the Windows and Android shells
    /// already have.
    ///
    /// Returns whether Funput handled the key. `false` is the safe answer, and the common
    /// one: the app then deletes its own character exactly as it did before, with nothing
    /// here having touched the document. `true` means the word *and* the character being
    /// deleted were replaced in a single `setMarkedText`, so the app must not delete again.
    ///
    /// Auto-repeat is left alone. Holding Backspace to wipe a phrase would otherwise
    /// re-open a word per repeat — an underline flickering through text on its way out —
    /// and would put a cross-process document read on every one of them. A deliberate
    /// single press never repeats, so the feature itself loses nothing.
    func reopenPreviousWord(_ client: IMKTextInput, event: NSEvent) -> Bool {
        guard AppSettings.shared.retoneAfterBackspace, !event.isARepeat else { return false }
        let document = IMKRetoneDocument(client: client)
        guard let plan = Retone.plan(document: document, adopt: composer.adopt) else {
            return false
        }
        setMarked(plan.word, client, replacementRange: plan.replacement)
        return true
    }
}
