//! The state a hook + inject shell keeps between keystrokes.
//!
//! One [`ShellState`] holds everything the platform's callbacks read and mutate:
//! the engine, the committed-text shadow behind retone-after-Backspace, the
//! persisted settings, and the per-app VI/EN bookkeeping. Every method takes
//! `&mut self` — the process-global and its mutex stay on the platform side,
//! because a hook callback is a bare `extern "system"` function with nowhere to
//! carry a pointer, and that is a Windows fact rather than a shell one.
//!
//! Keeping the state out of the global is what makes it testable: the rules below
//! (which app gets Vietnamese, what a pending toggle binds to, when composition is
//! dropped) are decisions no unit test could reach while they lived inside a
//! `OnceLock<Mutex<_>>`.
//!
//! # Layout
//!
//! - `config` — pushing settings into the engine, and persistence.
//! - `options` — the individual setting writes the Settings UI drives.
//! - `apps` — recent apps, per-app overrides, and the VI/EN auto-switch.
//! - `shortcuts` — the gõ tắt table, including half-typed drafts.
//! - `compose` — what the keyboard hook calls on every keystroke.

mod apps;
mod compose;
mod config;
mod options;
mod shortcuts;

use std::collections::HashMap;
use std::path::PathBuf;

use funput_config::{ExcludedApp, FlipHotkey, Hotkey, KeyCombo, Settings, Shortcut};
use funput_core::{InputMethod, ToneStyle as CoreToneStyle};
use funput_engine::Engine;

use crate::CommittedTail;

/// How many recently-focused apps to keep for the Settings "recent apps" picker.
const RECENT_CAP: usize = 12;

pub struct ShellState {
    engine: Engine,
    /// Shadow of the text typed into the focused app, up to the live composition.
    /// A hook shell cannot read the document, so this stands in for it when
    /// Backspace should re-open a finished word — see [`CommittedTail`].
    tail: CommittedTail,
    settings: Settings,
    /// Where `settings` is persisted, resolved once by the platform. `None` when no
    /// writable location exists — settings then live for this session only.
    settings_file: Option<PathBuf>,
    /// Recently-focused apps (most recent first), fed by the foreground hook. Not
    /// persisted — it's just a convenience source for the Settings UI.
    recent: Vec<ExcludedApp>,
    /// Per-app manual VI/EN overrides (runtime only, not persisted). A manual
    /// toggle records the user's choice here so the per-app auto-switch honours it
    /// on the next focus change instead of reverting to the exclusion-list default.
    /// Keyed by the platform's app id (on Windows, the lowercased exe name).
    overrides: HashMap<String, bool>,
    /// A manual toggle whose target app isn't known yet. The tray and the Settings
    /// window steal foreground, so the choice is parked here and bound to the next
    /// app the user focuses.
    pending_override: Option<bool>,
}

impl ShellState {
    /// Load `settings_file` and push it to a fresh engine. Missing or corrupt
    /// files fall back to defaults, so this cannot fail.
    pub fn new(settings_file: Option<PathBuf>) -> Self {
        let mut state = Self {
            engine: Engine::new(),
            tail: CommittedTail::new(),
            settings: Settings::default(),
            settings_file,
            recent: Vec::new(),
            overrides: HashMap::new(),
            pending_override: None,
        };
        state.settings = state.read_settings();
        state.apply_settings();
        state
    }

    // --- reads ---------------------------------------------------------------

    pub fn settings(&self) -> &Settings {
        &self.settings
    }
    pub fn enabled(&self) -> bool {
        self.settings.enabled
    }
    pub fn excluded_apps(&self) -> &[ExcludedApp] {
        &self.settings.excluded_apps
    }
    pub fn shortcuts(&self) -> &[Shortcut] {
        &self.settings.shortcuts
    }
    pub fn recent_apps(&self) -> &[ExcludedApp] {
        &self.recent
    }
    pub fn toggle_hotkey(&self) -> Hotkey {
        self.settings.toggle_hotkey
    }
    pub fn flip_hotkey(&self) -> FlipHotkey {
        self.settings.flip_hotkey
    }
    /// The user-recorded toggle combo, if one is set (overrides the preset).
    pub fn toggle_combo(&self) -> Option<&KeyCombo> {
        self.settings.toggle_combo.as_ref()
    }
    /// The user-recorded flip combo, if one is set (overrides the preset).
    pub fn flip_combo(&self) -> Option<&KeyCombo> {
        self.settings.flip_combo.as_ref()
    }
    pub fn method(&self) -> InputMethod {
        self.settings.method.core()
    }
    pub fn tone_style(&self) -> CoreToneStyle {
        self.settings.tone_style.core()
    }

    /// The app the user is in right now, or `None` before the first focus change.
    /// The platform uses this for per-app injection quirks.
    pub fn foreground_id(&self) -> Option<&str> {
        self.recent.first().map(|a| a.id.as_str())
    }
}

#[cfg(test)]
mod tests;
