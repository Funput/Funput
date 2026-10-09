//! What the Settings window and the tray call: read the current configuration,
//! and change one piece of it. Every setter persists — see
//! `funput_desktop::ShellState`, which owns the actual rules.

use funput_config::{ExtraOnsetLetters, FlipHotkey, Hotkey, KeyCombo, Settings, Shortcut};
use funput_core::{InputMethod, ToneStyle as CoreToneStyle};

use super::with;

/// Every write below goes through here: pick up the VI/EN state and app pins the
/// keyboard-hook process has written since this one read the file, *then* make
/// the change, under one lock. These writes run in the Settings and Control
/// Center processes, which read the file once at startup and save it whole — so
/// without the refresh, changing any option wrote back the pins from when the
/// window opened. See `ShellState::refresh_hook_state`.
fn write<R>(f: impl FnOnce(&mut funput_desktop::ShellState) -> R) -> R {
    with(|s| {
        s.refresh_hook_state();
        f(s)
    })
}

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
/// Whether the keyboard hook should stay in the key path at all — Vietnamese, or
/// English mode with gõ tắt still to do.
pub fn hook_active() -> bool {
    with(|s| s.hook_active())
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

// --- writes (each persists) ------------------------------------------------

pub fn reload_settings() -> bool {
    with(|s| s.reload_settings())
}
/// Pick up the hook process's VI/EN state and app pins — for a caller that takes a
/// [`snapshot`] to edit and hands it back through [`replace_settings`].
pub fn refresh_hook_state() {
    with(|s| s.refresh_hook_state());
}
pub fn replace_settings(new: Settings) {
    with(|s| s.replace_settings(new));
}
pub fn set_enabled(on: bool) {
    write(|s| s.set_enabled(on));
}
pub fn set_method(method: InputMethod) {
    write(|s| s.set_method(method));
}
pub fn set_tone_style(style: CoreToneStyle) {
    write(|s| s.set_tone_style(style));
}
pub fn set_smart_restore(on: bool) {
    write(|s| s.set_smart_restore(on));
}
pub fn set_eager_restore(on: bool) {
    write(|s| s.set_eager_restore(on));
}
pub fn set_spell_check(on: bool) {
    write(|s| s.set_spell_check(on));
}
pub fn set_auto_capitalize(on: bool) {
    write(|s| s.set_auto_capitalize(on));
}
pub fn set_extra_onsets(letters: ExtraOnsetLetters) {
    write(|s| s.set_extra_onsets(letters));
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
pub fn set_auto_english_on_foreign_layout(on: bool) {
    write(|s| s.set_auto_english_on_foreign_layout(on));
}
pub fn set_remember_app_language(on: bool) {
    write(|s| s.set_remember_app_language(on));
}
pub fn set_toggle_hotkey(hotkey: Hotkey) {
    write(|s| s.set_toggle_hotkey(hotkey));
}
pub fn set_toggle_combo(combo: KeyCombo) {
    write(|s| s.set_toggle_combo(combo));
}
pub fn set_flip_hotkey(hotkey: FlipHotkey) {
    write(|s| s.set_flip_hotkey(hotkey));
}
pub fn set_flip_combo(combo: KeyCombo) {
    write(|s| s.set_flip_combo(combo));
}
pub fn set_launch_at_login(on: bool) {
    write(|s| s.set_launch_at_login(on));
}
pub fn complete_onboarding() {
    write(|s| s.complete_onboarding());
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
