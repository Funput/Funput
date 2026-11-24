//! The gõ tắt table the way the Settings page lists it: newest row first.
//!
//! Rows are stored in the order they were added, and that order also settles two
//! rows that share a trigger: the one added first keeps firing (see [`super::check`]).
//! The page lists the table the other way up, so the row "Thêm" adds sits right
//! under the button in a table of any length, while a repeat typed into it is the
//! row that gets the error, never one the user already relies on further down.
//!
//! Every `position` here counts from the top of that listing.

use funput_config::Shortcut;

use super::check::{ShortcutNote, shortcut_notes};
use crate::shell::ShellState;

impl ShellState {
    /// The rows newest first, each with the note the page shows under it.
    pub fn listed_shortcuts(&self) -> Vec<(Shortcut, ShortcutNote)> {
        let rows = self.shortcuts();
        rows.iter()
            .cloned()
            .zip(shortcut_notes(rows))
            .rev()
            .collect()
    }

    /// The notes alone, in the same order, for refreshing them in place while a
    /// field keeps its focus.
    pub fn listed_shortcut_notes(&self) -> Vec<ShortcutNote> {
        let mut notes = shortcut_notes(self.shortcuts());
        notes.reverse();
        notes
    }

    /// Where the row listed at `position` is stored, or `None` past the end.
    pub fn listed_shortcut_index(&self, position: usize) -> Option<usize> {
        self.shortcuts().len().checked_sub(position)?.checked_sub(1)
    }
}
