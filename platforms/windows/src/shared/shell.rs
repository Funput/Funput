//! The process-global [`ShellState`], shared between the keyboard-hook thread, the
//! tray, and the UI callbacks.
//!
//! The state itself and every rule it enforces live in `funput-desktop`; this file
//! exists only because a `WH_KEYBOARD_LL` callback is a bare `extern "system"`
//! function with no user pointer to carry a handle in, so the state has to be
//! reachable from a static. Each function below is that static plus a lock.
//!
//! No Windows APIs here — the two Windows *facts* it does hold are the inject tag
//! and the browser list, both explained where they are defined.

use std::sync::{Mutex, OnceLock};

use funput_config::{ExcludedApp, FlipHotkey, Hotkey, KeyCombo, Settings, Shortcut};
use funput_core::{InputMethod, ToneStyle as CoreToneStyle};
use funput_desktop::{ImeResult, KeySource, ShellState};

use crate::shared::settings_path;

/// Tag stamped into `dwExtraInfo` of every event we synthesize via `SendInput`, so
/// the hook can recognize and ignore its own injected keystrokes (no re-entrancy).
pub const INJECT_TAG: usize = 0x4655_4E50; // "FUNP"

/// Browsers whose URL bar inline-autofills a *selected* suffix that eats a
/// synthesized Backspace (Chrome's omnibox, Firefox's address bar). Chrome
/// Beta/Dev/Canary also report `chrome.exe`; Firefox Developer Edition/Nightly
/// also report `firefox.exe`. Zen (Firefox fork) reports `zen.exe`. Edge
/// (`msedge.exe`) and Brave (`brave.exe`) deliberately do not match — they
/// are unaffected.
const URLBAR_AUTOFILL_BROWSERS: [&str; 3] = ["chrome.exe", "firefox.exe", "zen.exe"];

static SHELL: OnceLock<Mutex<ShellState>> = OnceLock::new();

fn shell() -> &'static Mutex<ShellState> {
    SHELL.get_or_init(|| Mutex::new(ShellState::new(settings_path::settings_path())))
}

fn with<R>(f: impl FnOnce(&mut ShellState) -> R) -> R {
    let mut guard = shell().lock().expect("shell mutex poisoned");
    f(&mut guard)
}

// --- reads -----------------------------------------------------------------

pub fn snapshot() -> Settings {
    with(|s| s.settings().clone())
}
pub fn excluded_apps() -> Vec<ExcludedApp> {
    with(|s| s.excluded_apps().to_vec())
}
pub fn recent_apps() -> Vec<ExcludedApp> {
    with(|s| s.recent_apps().to_vec())
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

/// True when the focused app is a browser whose URL bar autofill swallows
/// synthesized Backspaces. Used to route text injection through the Delete-primer
/// path (see `inject::send_plan_primed`).
pub fn foreground_has_urlbar_autofill() -> bool {
    with(|s| {
        s.foreground_id()
            .is_some_and(|id| URLBAR_AUTOFILL_BROWSERS.contains(&id))
    })
}

// --- writes ----------------------------------------------------------------

pub fn reload_settings() -> bool {
    with(|s| s.reload_settings())
}
pub fn replace_settings(new: Settings) {
    with(|s| s.replace_settings(new));
}
pub fn seed_recent_apps(apps: Vec<ExcludedApp>) {
    with(|s| s.set_recent_apps(apps));
}
pub fn toggle_enabled() -> bool {
    with(|s| s.toggle_enabled())
}
pub fn toggle_enabled_hotkey() -> bool {
    with(|s| s.toggle_enabled_hotkey())
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
pub fn add_excluded_app(app: ExcludedApp) {
    with(|s| s.add_excluded_app(app));
}
pub fn remove_excluded_app(id: &str) {
    with(|s| s.remove_excluded_app(id));
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

// --- per-app auto-switch + composition (called from the hook) ---------------

pub fn note_foreground(id: String, name: String) {
    with(|s| s.note_foreground(id, name));
}
pub fn apply_for_app(id: &str) -> Option<bool> {
    with(|s| s.apply_for_app(id))
}
pub fn process_key(c: char, source: KeySource) -> ImeResult {
    with(|s| s.process_key(c, source))
}
pub fn flip_composing() -> ImeResult {
    with(|s| s.flip_composing())
}
pub fn on_backspace() {
    with(|s| s.on_backspace());
}
pub fn arm_capitalization() {
    with(|s| s.arm_capitalization());
}
pub fn clear() {
    with(|s| s.clear());
}
