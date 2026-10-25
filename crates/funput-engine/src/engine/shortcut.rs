//! One-tap undo of a gõ tắt expansion, for hosts that draw a suggestion bar.
//!
//! [`Engine::on_backspace`] does not take it. A desktop shell that deletes the
//! character itself would otherwise undo an expansion the user cannot see a chip for.
//! Android asks [`Engine::undo_shortcut`] when Backspace is still the next key.

use crate::compose::boundary::shortcut_undo;
use crate::{Engine, ImeResult};

impl Engine {
    /// Whether Backspace can still put the last expansion back.
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

    /// Drop the undo and any one-time refusal. The caret has left this field.
    pub fn dismiss_shortcut_undo(&mut self) {
        shortcut_undo::dismiss(&mut self.session);
    }
}
