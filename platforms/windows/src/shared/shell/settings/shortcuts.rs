//! The gõ tắt table and its switches, for the Settings window.
//!
//! Every `index` below is a row's stored index, which each listed row carries;
//! the page's order is not storage order (see `ShellState::listed_shortcuts`).

use funput_desktop::{ListedShortcut, ShortcutNote};

use super::super::with;
use super::write;

/// The rows in the page's order, each with its stored index and its note.
pub fn listed_shortcuts() -> Vec<ListedShortcut> {
    with(|s| s.listed_shortcuts())
}
/// Every row's note, by stored index.
pub fn shortcut_notes() -> Vec<ShortcutNote> {
    with(|s| funput_desktop::shortcut_notes(s.shortcuts()))
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
pub fn remove_shortcut(index: usize) {
    write(|s| s.remove_shortcut(index));
}
pub fn set_shortcut_trigger(index: usize, trigger: String) {
    write(|s| s.set_shortcut_trigger(index, trigger));
}
pub fn set_shortcut_expansion(index: usize, expansion: String) {
    write(|s| s.set_shortcut_expansion(index, expansion));
}
