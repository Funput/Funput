//! The gõ tắt table and its switches, for the Settings window.

use funput_config::Shortcut;

use super::super::with;
use super::write;

pub fn shortcuts() -> Vec<Shortcut> {
    with(|s| s.shortcuts().to_vec())
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
