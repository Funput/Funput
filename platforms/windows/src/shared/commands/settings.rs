//! Straight passthroughs from a UI control to the shared shell state.
//!
//! Each one exists so a Slint callback can name a single function instead of
//! reaching into `shell` and converting types itself.

use funput_config::{FlipHotkey, Hotkey, KeyCombo, Method, ToneStyle};

use crate::shared::shell;

pub fn set_method(method: Method) {
    shell::set_method(method.core());
}

pub fn set_tone_style(tone_style: ToneStyle) {
    shell::set_tone_style(tone_style.core());
}

pub fn set_smart_restore(on: bool) {
    shell::set_smart_restore(on);
}

pub fn set_eager_restore(on: bool) {
    shell::set_eager_restore(on);
}

pub fn set_spell_check(on: bool) {
    shell::set_spell_check(on);
}

pub fn set_auto_capitalize(on: bool) {
    shell::set_auto_capitalize(on);
}

pub fn set_shortcuts_enabled(on: bool) {
    shell::set_shortcuts_enabled(on);
}

pub fn set_auto_english_layout(on: bool) {
    shell::set_auto_english_on_foreign_layout(on);
}

pub fn set_enabled(on: bool) {
    shell::set_enabled(on);
}

pub fn set_toggle_hotkey(hotkey: Hotkey) {
    shell::set_toggle_hotkey(hotkey);
}

pub fn set_flip_hotkey(hotkey: FlipHotkey) {
    shell::set_flip_hotkey(hotkey);
}

pub fn set_toggle_combo(combo: KeyCombo) {
    shell::set_toggle_combo(combo);
}

pub fn set_flip_combo(combo: KeyCombo) {
    shell::set_flip_combo(combo);
}

pub fn complete_onboarding() {
    shell::complete_onboarding();
}

// --- Shortcuts (gõ tắt) -----------------------------------------------------

pub fn add_shortcut() {
    shell::add_shortcut();
}

pub fn can_add_shortcut() -> bool {
    shell::can_add_shortcut()
}

pub fn prune_incomplete_shortcuts() {
    shell::prune_incomplete_shortcuts();
}

pub fn remove_shortcut(index: usize) {
    shell::remove_shortcut(index);
}

pub fn set_shortcut_trigger(index: usize, trigger: String) {
    shell::set_shortcut_trigger(index, trigger);
}

pub fn set_shortcut_expansion(index: usize, expansion: String) {
    shell::set_shortcut_expansion(index, expansion);
}
