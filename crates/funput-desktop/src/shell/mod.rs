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
//! - `apps` — the focused app, the per-app VI/EN memory, and the auto-switch.
//! - `shortcuts` — the gõ tắt table, including half-typed drafts.
//! - `compose` — what the keyboard hook calls on every keystroke.

mod apps;
mod compose;
mod config;
mod options;
mod shortcuts;

use std::path::PathBuf;

use funput_config::{FlipHotkey, Hotkey, KeyCombo, Settings, Shortcut};
use funput_core::{InputMethod, ToneStyle as CoreToneStyle};
use funput_engine::Engine;

use crate::CommittedTail;

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
    /// The app that currently has focus, fed by the foreground hook. Not
    /// persisted, and `None` until the first focus change.
    foreground: Option<String>,
    /// A manual toggle whose target app isn't known yet. The tray and the Settings
    /// window steal foreground, so the choice is parked here and bound to the next
    /// app the user focuses. Session-only: re-arming a stale choice after a
    /// restart would be surprising, so it is never persisted.
    pending_override: Option<bool>,
    /// The keyboard layout handle last judged by [`Self::apply_for_layout`], so an
    /// unchanged layout costs nothing. `0` until the platform reports one.
    last_layout: u32,
    /// Vietnamese is suspended because the focused layout is one it cannot be typed
    /// on. Not a settings field and never persisted: it describes the keyboard in
    /// front of the user right now, and is recomputed from it — see
    /// [`Self::effective_enabled`].
    layout_suspended: bool,
    /// The one foreign layout the user has overruled by turning Vietnamese back on
    /// inside it. Kept until they move to a different layout, so Funput states its
    /// case once and then lets them type. `0` when there is none.
    layout_override: u32,
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
            foreground: None,
            pending_override: None,
            last_layout: 0,
            layout_suspended: false,
            layout_override: 0,
        };
        state.settings = state.read_settings();
        state.apply_settings();
        state
    }

    // --- reads ---------------------------------------------------------------

    pub fn settings(&self) -> &Settings {
        &self.settings
    }
    /// Whether Vietnamese is actually being typed right now — the user's choice
    /// *and* the layout's veto. This is what the hook and the tray ask; the
    /// Settings window reads `settings().enabled` instead, because it wants the
    /// choice, not the momentary state of a keyboard it is not focused on.
    pub fn enabled(&self) -> bool {
        self.effective_enabled()
    }
    pub fn shortcuts(&self) -> &[Shortcut] {
        &self.settings.shortcuts
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
    /// What a hotkey toggle binds its VI/EN choice to.
    pub fn foreground_id(&self) -> Option<&str> {
        self.foreground.as_deref()
    }
}

#[cfg(test)]
mod tests;
