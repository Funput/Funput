import Foundation

/// Backspace right after a gõ tắt expansion puts the trigger back: `vn` + Space + ⌫
/// shows `vn ` again instead of `Việt Nam`, as on Windows, Android and iOS.
///
/// The engine keeps the undo armed until the next key, and refuses the trigger once after
/// it is taken. This adds the macOS half of the rule every host follows: be sure the
/// expansion is still right before the caret before asking, because asking spends it. A
/// committed word has no marked text, so IMKit never reports a click that moved the
/// caret; a click since the expansion therefore drops the undo, the same signal
/// `CompositionSession.endIfClicked` reads for a live word.
struct ShortcutUndoSession {
    /// Clicks seen so far, anywhere.
    private let clicks: () -> Int
    /// The click count when the armed undo was first seen; nil while nothing is armed.
    private var clicksAtExpansion: Int?

    init(clicks: @escaping () -> Int) {
        self.clicks = clicks
    }

    /// After every key: note the click count once an expansion arms the undo, and forget
    /// it when the undo is gone. The counter is read only while an undo is armed, so its
    /// global monitor is never installed for someone who does not use gõ tắt.
    mutating func keyHandled(by composer: FunputComposer) {
        guard composer.hasShortcutUndo else {
            clicksAtExpansion = nil
            return
        }
        if clicksAtExpansion == nil { clicksAtExpansion = clicks() }
    }

    /// Backspace with nothing composing. True when the expansion was replaced by the word
    /// it came from, so the app must not delete a character as well. False leaves the key
    /// to the app; an undo that no longer applies is dropped rather than left to rewrite
    /// text that may not be there.
    func backspace(composer: FunputComposer, on surface: CompositionSurface) -> Bool {
        guard composer.hasShortcutUndo else { return false }
        guard clicksAtExpansion == clicks(),
            let written = composer.shortcutUndoWritten(),
            let range = ShortcutUndoDocument.range(of: written, on: surface)
        else {
            composer.dismissShortcutUndo()
            return false
        }
        guard let restored = composer.undoShortcut() else { return false }
        surface.insert(restored, replacing: range)
        return true
    }
}
