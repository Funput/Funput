//! The gõ tắt table and its switches, for the Settings window.
//!
//! The page lists the table newest first, so every row `position` below counts
//! from the top of that listing; the shell stores rows oldest first. See
//! `funput_desktop`'s `shortcuts::listing` for why the two orders differ.

use funput_config::Shortcut;
use funput_desktop::{ShellState, ShortcutNote};

use super::super::with;
use super::write;

pub fn listed_shortcuts() -> Vec<(Shortcut, ShortcutNote)> {
    with(|s| s.listed_shortcuts())
}
pub fn listed_shortcut_notes() -> Vec<ShortcutNote> {
    with(|s| s.listed_shortcut_notes())
}
pub fn can_add_shortcut() -> bool {
    with(|s| s.can_add_shortcut())
}
pub fn set_shortcuts_enabled(on: bool) {
    write(|s| s.set_shortcuts_enabled(on));
}
pub fn set_shortcut_smart_case(on: bool) {
    write(|s| s.set_shortcut_smart_case(on));
}
pub fn set_shortcuts_in_english(on: bool) {
    write(|s| s.set_shortcuts_in_english(on));
}
pub fn set_shortcut_on_enter(on: bool) {
    write(|s| s.set_shortcut_on_enter(on));
}
pub fn set_shortcut_on_tab(on: bool) {
    write(|s| s.set_shortcut_on_tab(on));
}
pub fn add_shortcut() {
    write(|s| s.add_shortcut());
}
pub fn prune_incomplete_shortcuts() {
    write(|s| s.prune_incomplete_shortcuts());
}
pub fn remove_shortcut(position: usize) {
    edit_listed(position, |s, index| s.remove_shortcut(index));
}
pub fn set_shortcut_trigger(position: usize, trigger: String) {
    edit_listed(position, |s, index| s.set_shortcut_trigger(index, trigger));
}
pub fn set_shortcut_expansion(position: usize, expansion: String) {
    edit_listed(position, |s, index| {
        s.set_shortcut_expansion(index, expansion)
    });
}

/// Apply `f` to the row listed at `position`, resolved under the same lock as the
/// edit. A position past the end is ignored, as the shell ignores a bad index.
fn edit_listed(position: usize, f: impl FnOnce(&mut ShellState, usize)) {
    write(|s| {
        if let Some(index) = s.listed_shortcut_index(position) {
            f(s, index);
        }
    });
}
