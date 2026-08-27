//! What the Settings window and the tray call: read the current configuration,
//! and change one piece of it. Every setter persists — see
//! `funput_desktop::ShellState`, which owns the actual rules.

use funput_config::{FlipHotkey, Hotkey, KeyCombo, Settings, Shortcut};
use funput_core::{InputMethod, ToneStyle as CoreToneStyle};

use super::with;

// --- reads -----------------------------------------------------------------

pub fn snapshot() -> Settings {
    with(|s| s.settings().clone())
}
pub fn shortcuts() -> Vec<Shortcut> {
    with(|s| s.shortcuts().to_vec())
}
pub fn enabled() -> bool {
    with(|s| s.enabled())
}
pub fn method() -> InputMethod {
    with(|s| s.method())
}
pub fn tone_style() -> CoreToneStyle {
    with(|s| s.tone_style())
}
pub fn toggle_hotkey() -> Hotkey {
    with(|s| s.toggle_hotkey())
}
pub fn flip_hotkey() -> FlipHotkey {
    with(|s| s.flip_hotkey())
}
pub fn toggle_combo() -> Option<KeyCombo> {
    with(|s| s.toggle_combo().cloned())
}
pub fn flip_combo() -> Option<KeyCombo> {
    with(|s| s.flip_combo().cloned())
}
pub fn can_add_shortcut() -> bool {
    with(|s| s.can_add_shortcut())
}

/// The user's current input method + tone style, for the in-process composer that
/// the Settings window's gõ tắt field uses (it can't go through the global hook).
pub fn method_and_tone() -> (InputMethod, CoreToneStyle) {
    with(|s| (s.method(), s.tone_style()))
}

// --- writes (each persists) ------------------------------------------------

pub fn reload_settings() -> bool {
    with(|s| s.reload_settings())
}
pub fn replace_settings(new: Settings) {
    with(|s| s.replace_settings(new));
}
pub fn set_enabled(on: bool) {
    with(|s| s.set_enabled(on));
}
pub fn set_method(method: InputMethod) {
    with(|s| s.set_method(method));
}
pub fn set_tone_style(style: CoreToneStyle) {
    with(|s| s.set_tone_style(style));
}
pub fn set_smart_restore(on: bool) {
    with(|s| s.set_smart_restore(on));
}
pub fn set_eager_restore(on: bool) {
    with(|s| s.set_eager_restore(on));
}
pub fn set_spell_check(on: bool) {
    with(|s| s.set_spell_check(on));
}
pub fn set_auto_capitalize(on: bool) {
    with(|s| s.set_auto_capitalize(on));
}
pub fn set_shortcuts_enabled(on: bool) {
    with(|s| s.set_shortcuts_enabled(on));
}
pub fn set_auto_english_on_foreign_layout(on: bool) {
    with(|s| s.set_auto_english_on_foreign_layout(on));
}
pub fn set_toggle_hotkey(hotkey: Hotkey) {
    with(|s| s.set_toggle_hotkey(hotkey));
}
pub fn set_toggle_combo(combo: KeyCombo) {
    with(|s| s.set_toggle_combo(combo));
}
pub fn set_flip_hotkey(hotkey: FlipHotkey) {
    with(|s| s.set_flip_hotkey(hotkey));
}
pub fn set_flip_combo(combo: KeyCombo) {
    with(|s| s.set_flip_combo(combo));
}
pub fn set_launch_at_login(on: bool) {
    with(|s| s.set_launch_at_login(on));
}
pub fn complete_onboarding() {
    with(|s| s.complete_onboarding());
}
pub fn add_shortcut() {
    with(|s| s.add_shortcut());
}
pub fn prune_incomplete_shortcuts() {
    with(|s| s.prune_incomplete_shortcuts());
}
pub fn remove_shortcut(index: usize) {
    with(|s| s.remove_shortcut(index));
}
pub fn set_shortcut_trigger(index: usize, trigger: String) {
    with(|s| s.set_shortcut_trigger(index, trigger));
}
pub fn set_shortcut_expansion(index: usize, expansion: String) {
    with(|s| s.set_shortcut_expansion(index, expansion));
}
