//! Gõ tắt calls a host makes itself: one-tap undo, where Backspace right after an
//! expansion puts the trigger back, and expansion before Enter or Tab.
//!
//! [`Engine::on_backspace`] does not take the undo, because only the host knows whether
//! the expansion still sits before the caret. Android and iOS check the document;
//! the desktop shell checks its shadow of what it typed. Each then asks
//! [`Engine::undo_shortcut`] when Backspace is still the next key, or first
//! [`Engine::rearm_shortcut_undo`] when the user typed on and deleted back to it.

use crate::compose::boundary::{self, shortcut_undo};
use crate::{Engine, ImeResult};

impl Engine {
    /// Whether the undo is armed: Backspace can put the last expansion back right now.
    pub fn has_shortcut_undo(&self) -> bool {
        shortcut_undo::is_armed(&self.session)
    }

    /// The word as it was shown before the expansion. What the `↩` chip names.
    pub fn shortcut_undo_text(&self) -> Option<&str> {
        shortcut_undo::shown(&self.session)
    }

    /// Expansion plus the boundary character. The document must still end with this.
    pub fn shortcut_undo_written(&self) -> Option<&str> {
        shortcut_undo::written(&self.session)
    }

    /// Replace the expansion with the shown word and the same boundary character.
    /// The trigger is then kept once, the next time it is typed.
    pub fn undo_shortcut(&mut self) -> Option<ImeResult> {
        shortcut_undo::undo(&mut self.session)
    }

    /// Arm the undo of the last expansion again, after the next key spent it: the user
    /// typed on, then deleted back until the caret sits right after the expansion
    /// (`cơm tấm␣này` ⌫⌫⌫ → `cơm tấm␣`). Returns whether an undo is now armed.
    ///
    /// Only the host can tell the caret is back at that exact spot, not at the same
    /// text typed somewhere else, so it checks before calling. `false` while a word is
    /// being composed, or once the expansion was taken back, dismissed, or cleared.
    pub fn rearm_shortcut_undo(&mut self) -> bool {
        shortcut_undo::rearm(&mut self.session)
    }

    /// Expand the live word if it is a gõ tắt trigger, for a key that ends the word
    /// but that the host sends on to the app itself: Enter, which may send a message,
    /// or Tab, which may move focus. The result has no boundary character, so the host
    /// injects it and then the real key. `None` leaves the word as typed.
    ///
    /// Only gõ tắt runs: no English restore and no typo correction, and no undo is
    /// armed. On a match the word is committed; otherwise the host commits it as it
    /// does for any key that moves the caret.
    pub fn expand_shortcut(&mut self) -> Option<ImeResult> {
        if !self.session.enabled && !self.session.english_shortcuts() {
            return None;
        }
        let result = boundary::expansion_in_place(&mut self.session)?;
        self.session.clear();
        Some(result)
    }

    /// Drop the undo, the remembered expansion and any one-time refusal: the caret has
    /// left this field, or the expansion is no longer what sits before it.
    pub fn dismiss_shortcut_undo(&mut self) {
        shortcut_undo::dismiss(&mut self.session);
    }
}
