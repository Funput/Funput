//! The process-global [`ShellState`], shared between the keyboard-hook thread, the
//! tray, and the UI callbacks.
//!
//! The state itself and every rule it enforces live in `funput-desktop`; this
//! module exists only because a `WH_KEYBOARD_LL` callback is a bare `extern
//! "system"` function with no user pointer to carry a handle in, so the state has
//! to be reachable from a static. Each function here is that static plus a lock.
//!
//! What the hook calls stays here; what the Settings window and tray call lives in
//! [`settings`]. No Windows APIs in either — the two Windows *facts* this file
//! does hold are the inject tag and the browser list, both explained below.

mod settings;

use std::sync::{Mutex, OnceLock};

use funput_config::ExcludedApp;
use funput_desktop::{ImeResult, KeySource, ShellState};

use crate::shared::settings_path;

pub use settings::*;

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

pub(super) fn with<R>(f: impl FnOnce(&mut ShellState) -> R) -> R {
    let mut guard = shell().lock().expect("shell mutex poisoned");
    f(&mut guard)
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

// --- called from the hook --------------------------------------------------

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
pub fn note_foreground(id: String, name: String) {
    with(|s| s.note_foreground(id, name));
}
pub fn apply_for_app(id: &str) -> Option<bool> {
    with(|s| s.apply_for_app(id))
}
/// Flip VI/EN from the keyboard hotkey; returns the new state.
pub fn toggle_enabled_hotkey() -> bool {
    with(|s| s.toggle_enabled_hotkey())
}
/// Seed a UI child with the background process's runtime-only recent-app list.
pub fn seed_recent_apps(apps: Vec<ExcludedApp>) {
    with(|s| s.set_recent_apps(apps));
}
