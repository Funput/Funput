//! The process-global [`ShellState`], shared between the keyboard-hook thread, the
//! tray, and the UI callbacks.
//!
//! The state itself and every rule it enforces live in `funput-desktop`; this
//! module exists only because a `WH_KEYBOARD_LL` callback is a bare `extern
//! "system"` function with no user pointer to carry a handle in, so the state has
//! to be reachable from a static. Each function here is that static plus a lock.
//!
//! What the hook calls stays here; what the Settings window and tray call lives in
//! [`settings`]. No Windows APIs in either — the one Windows *fact* this file does
//! hold is the inject tag, explained below.

mod settings;

use std::sync::{Mutex, OnceLock};

use funput_desktop::{ImeResult, KeySource, ShellState};

use crate::shared::settings_path;

pub use settings::*;

/// Tag stamped into `dwExtraInfo` of every event we synthesize via `SendInput`, so
/// the hook can recognize and ignore its own injected keystrokes (no re-entrancy).
pub const INJECT_TAG: usize = 0x4655_4E50; // "FUNP"

static SHELL: OnceLock<Mutex<ShellState>> = OnceLock::new();

fn shell() -> &'static Mutex<ShellState> {
    SHELL.get_or_init(|| Mutex::new(ShellState::new(settings_path::settings_path())))
}

pub(super) fn with<R>(f: impl FnOnce(&mut ShellState) -> R) -> R {
    let mut guard = shell().lock().expect("shell mutex poisoned");
    f(&mut guard)
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
pub fn note_foreground(id: String) {
    with(|s| s.note_foreground(id));
}
pub fn apply_for_app(id: &str) -> Option<bool> {
    with(|s| s.apply_for_app(id))
}
/// Flip VI/EN from the keyboard hotkey; returns the new state. In memory only —
/// [`save_settings`] finishes the job off the hook.
pub fn toggle_enabled_hotkey() -> bool {
    with(|s| s.toggle_enabled_hotkey())
}
/// Persist what a hotkey toggle changed. Runs on the message pump, never in the
/// hook: see [`crate::background::hook`]'s `toggle` module.
pub fn save_settings() {
    with(|s| s.save_settings());
}
