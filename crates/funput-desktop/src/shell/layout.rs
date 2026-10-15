//! Which keyboard gets Vietnamese.
//!
//! Next to the per-app memory in [`super::apps`], but a rule rather than a
//! recollection: Vietnamese cannot be typed on a CJK IME or a non-Latin layout at
//! all (see [`crate::is_foreign_layout`]), so Funput steps aside on one without
//! being told, and without writing anything down.

use super::ShellState;

impl ShellState {
    /// Decide VI/EN for the keyboard layout under the caret, from its handle.
    ///
    /// It is the *last* word — the platform asks it after [`Self::apply_for_app`],
    /// so it outranks a remembered choice.
    ///
    /// Nothing is persisted: the suspension describes the keyboard in front of the
    /// user, would be stale by the next launch, and layouts change far too often to
    /// spend a file write on. Returns `Some(on)` only when Vietnamese actually
    /// started or stopped running, so the caller can refresh its tray.
    pub fn apply_for_layout(&mut self, layout: u32) -> Option<bool> {
        if layout == self.last_layout {
            return None;
        }
        self.last_layout = layout;
        // An override earns its keep only on the layout it was granted for.
        if layout != self.layout_override {
            self.layout_override = 0;
        }
        let suspend = self.settings.auto_english_on_foreign_layout
            && layout != self.layout_override
            && crate::is_foreign_layout(layout);
        if suspend == self.layout_suspended {
            return None;
        }
        let before = self.effective_enabled();
        self.layout_suspended = suspend;
        let after = self.effective_enabled();
        if after == before {
            return None; // the user is in English anyway; nothing to suspend
        }
        self.engine.set_enabled(after);
        // Someone else's composition owns the caret now (or did), so the shadow of
        // what Funput typed no longer describes the text sitting in front of it.
        self.reset_composition();
        Some(after)
    }
}
