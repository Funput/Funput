import Foundation

/// Backspace right after a gõ tắt expansion puts the trigger back: `vn` + Space + ⌫
/// shows `vn ` again instead of `Việt Nam`, as on Windows, Android and iOS. So does
/// Backspace once the user typed on and deleted back to the expansion's end (#782).
///
/// The engine keeps the undo armed until the next key, remembers the expansion past it,
/// and refuses the trigger once after it is taken. This adds the macOS half of the rule
/// every host follows: be sure the expansion is still right before the caret before
/// asking, because asking spends it. A committed word has no marked text, so IMKit never
/// reports a click that moved the caret; a click since the expansion therefore drops it,
/// the same signal `CompositionSession.endIfClicked` reads for a live word.
struct ShortcutUndoSession {
    private struct Expansion {
        /// The click count when the expansion landed.
        let clicks: Int
        /// The caret right after it (UTF-16), or nil when the client would not say.
        let end: Int?
    }

    /// Clicks seen so far, anywhere.
    private let clicks: () -> Int
    /// The last expansion; nil once there is nothing left to take back.
    private var expansion: Expansion?
    /// Whether the undo was armed after the previous key, so one expansion is noted once.
    private var wasArmed = false

    init(clicks: @escaping () -> Int) {
        self.clicks = clicks
    }

    /// After every key: note the click count and the caret once an expansion arms the
    /// undo. Both are read only then, so the click monitor is never installed, and the app
    /// never asked, for someone who does not use gõ tắt.
    mutating func keyHandled(by composer: FunputComposer, on surface: CompositionSurface) {
        let armed = composer.hasShortcutUndo
        defer { wasArmed = armed }
        guard armed, !wasArmed else { return }
        expansion = Expansion(clicks: clicks(), end: ShortcutUndoDocument.caret(on: surface))
    }

    /// Backspace with nothing composing. True when the expansion was replaced by the word
    /// it came from, so the app must not delete a character as well. False leaves the key
    /// to the app; an undo that no longer applies is dropped rather than left to rewrite
    /// text that may not be there.
    mutating func backspace(composer: FunputComposer, on surface: CompositionSurface) -> Bool {
        guard let expansion else { return false }
        guard expansion.clicks == clicks() else { return forget(composer) }
        if !composer.hasShortcutUndo {
            guard let end = expansion.end, ShortcutUndoDocument.caret(on: surface) == end
            else { return false }
            guard composer.rearmShortcutUndo() else { return forget(composer) }
        }
        guard let written = composer.shortcutUndoWritten(),
            let range = ShortcutUndoDocument.range(of: written, on: surface),
            let restored = composer.undoShortcut()
        else { return forget(composer) }
        self.expansion = nil
        surface.insert(restored, replacing: range)
        return true
    }

    /// The expansion is no longer known to sit before the caret: drop it, in the engine too.
    private mutating func forget(_ composer: FunputComposer) -> Bool {
        expansion = nil
        composer.dismissShortcutUndo()
        return false
    }
}
