//! The gõ tắt table the way the Settings page lists it.
//!
//! Rows are stored in the order they were added, and that order also settles two
//! rows that share a trigger: the one added first keeps firing (see [`super::check`]).
//! The page lists them for reading instead: the row "Thêm" just added sits first,
//! right under the button, and every other row follows by trigger. Each listed row
//! carries its stored index, which is what every edit takes.

use funput_config::Shortcut;
use funput_core::textcase::{self, Transform};

use super::check::{ShortcutNote, shortcut_notes};
use super::is_complete;
use crate::shell::ShellState;

/// One row as the page lists it.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct ListedShortcut {
    /// Where the row is stored: the index every edit and removal takes.
    pub index: usize,
    pub shortcut: Shortcut,
    pub note: ShortcutNote,
}

impl ShellState {
    /// The rows in the page's order, each with its stored index and its note.
    pub fn listed_shortcuts(&self) -> Vec<ListedShortcut> {
        let rows = self.shortcuts();
        let mut notes = shortcut_notes(rows);
        listing_order(rows)
            .into_iter()
            .map(|index| ListedShortcut {
                index,
                shortcut: rows[index].clone(),
                note: std::mem::take(&mut notes[index]),
            })
            .collect()
    }
}

/// Stored indexes in listing order.
///
/// Only the newest row, while it is still being typed, is pinned first. Any other
/// row stays where its trigger puts it, even emptied for retyping, so it does not
/// jump to the top. Equal triggers keep stored order, which lists the row that
/// fires above its repeat.
fn listing_order(rows: &[Shortcut]) -> Vec<usize> {
    let draft = rows
        .len()
        .checked_sub(1)
        .filter(|&newest| !is_complete(&rows[newest]));
    let mut sorted: Vec<usize> = (0..rows.len()).filter(|&i| Some(i) != draft).collect();
    sorted.sort_by_cached_key(|&index| (sort_key(&rows[index].trigger), index));
    draft.into_iter().chain(sorted).collect()
}

/// Sorts the way a reader looks a trigger up: by its letters without tone marks
/// or case (`đi` next to `di`, `Ăn` next to `an`), then by the exact lowercase
/// spelling so the plain letter comes before its marked forms.
fn sort_key(trigger: &str) -> (String, String) {
    let lower = trigger.to_lowercase();
    let letters = textcase::apply(&lower, Transform::NoDiacritics, Default::default());
    (letters, lower)
}
