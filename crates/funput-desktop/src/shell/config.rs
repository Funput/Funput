//! Getting settings into the engine, and onto disk.
//!
//! `settings` is the single source of truth: every write elsewhere mutates a field
//! and then re-syncs through here, so the engine can never drift from what was
//! persisted.

use funput_config::{Settings, Shortcut};
use funput_engine::{Engine, EngineConfig};

use super::ShellState;

impl ShellState {
    /// Push everything in `settings` to the engine and start from a clean slate.
    pub(super) fn apply_settings(&mut self) {
        self.sync_engine_config();
        self.engine.set_enabled(self.effective_enabled());
        push_shortcuts(&mut self.engine, &self.settings.shortcuts);
        self.reset_composition();
    }

    /// Push the engine options in one call. `enabled` and the gõ tắt *rows* are
    /// separate (see [`ShellState::set_enabled_state`] and [`push_shortcuts`]); the
    /// switch that decides whether those rows expand is an option and rides here.
    pub(super) fn sync_engine_config(&mut self) {
        self.engine.configure(EngineConfig {
            method: self.settings.method.core(),
            tone_style: self.settings.tone_style.core(),
            smart_restore: self.settings.smart_restore,
            eager_restore: self.settings.eager_restore,
            spell_check: self.settings.spell_check,
            auto_capitalize: self.settings.auto_capitalize,
            shortcuts_enabled: self.settings.shortcuts_enabled,
        });
    }

    /// Drop the live composition *and* the committed-text shadow together. The two
    /// model one stretch of text around the caret, so one must never outlive the
    /// other: a shadow left standing after the engine forgot the word would let
    /// Backspace re-open text that is no longer where the shell thinks it is.
    pub(super) fn reset_composition(&mut self) {
        self.engine.clear();
        self.tail.clear();
    }

    /// Whether Vietnamese is actually running: the user asked for it *and* the
    /// focused keyboard layout is one it can be typed on. The suspension is the
    /// only thing that can override the setting, and it never writes to it — so a
    /// layout change costs no disk write, and `reload_settings` below cannot
    /// mistake a suspended session for the user having flipped VI/EN elsewhere.
    pub(super) fn effective_enabled(&self) -> bool {
        self.settings.enabled && !self.layout_suspended
    }

    /// Apply a VI/EN state to both the persisted settings and the live engine.
    /// Callers persist themselves, since some batch this with other field writes.
    pub(super) fn set_enabled_state(&mut self, on: bool) {
        self.settings.enabled = on;
        self.engine.set_enabled(self.effective_enabled());
        // Both directions: English-mode keystrokes never reach the engine, so
        // whatever the shadow held no longer describes the text at the caret.
        self.reset_composition();
    }

    /// Read the settings file (defaults when it is missing, corrupt, or there is
    /// nowhere to keep one).
    pub(super) fn read_settings(&self) -> Settings {
        self.settings_file
            .as_deref()
            .map(Settings::load_from)
            .unwrap_or_default()
    }

    /// Persist the current settings.
    pub(super) fn save(&self) {
        if let Some(path) = &self.settings_file {
            self.settings.save_to(path);
        }
    }

    /// Reload settings written by another process (the Settings window and the
    /// Control Center each run as their own). The focused app is untouched; only
    /// persisted state and the live engine are refreshed. Returns whether anything
    /// actually changed, so the caller can skip refreshing its UI.
    pub fn reload_settings(&mut self) -> bool {
        let loaded = self.read_settings();
        if self.settings == loaded {
            return false;
        }
        // A VI/EN flip made in one of those windows parked itself in *their*
        // `ShellState`, which died with the process — so the app it was meant for
        // never learned about it. Park it here instead and the next focus change
        // binds it, exactly as an in-process toggle would.
        if loaded.enabled != self.settings.enabled {
            self.pending_override = Some(loaded.enabled);
        }
        self.settings = loaded;
        self.apply_settings();
        // The foreign-layout switch may have been the thing that changed, and the
        // layout it applies to has not moved — re-judge it rather than waiting.
        self.redecide_layout();
        true
    }

    /// Replace all settings at once (config import). Applies to the live engine and
    /// persists; another process picks the change up via [`Self::reload_settings`].
    pub fn replace_settings(&mut self, new: Settings) {
        self.settings = new;
        self.apply_settings();
        self.save();
    }

    /// Re-run the layout rule against the layout already in front of the user,
    /// after the settings changed under it: turning the auto-switch off has to give
    /// Vietnamese back now, not at the next change of keyboard.
    pub(super) fn redecide_layout(&mut self) {
        let layout = std::mem::take(&mut self.last_layout);
        self.apply_for_layout(layout);
    }

    /// Mutate one engine option, then re-sync the whole config and persist. Folding
    /// the three steps here makes it impossible to change a setting without pushing
    /// it to the engine.
    pub(super) fn update_config(&mut self, edit: impl FnOnce(&mut Settings)) {
        edit(&mut self.settings);
        self.sync_engine_config();
        self.save();
    }
}

/// Replace the engine's gõ tắt table (empty triggers are skipped by the engine).
/// The whole table is re-pushed after any edit — it's small and cheap.
pub(super) fn push_shortcuts(engine: &mut Engine, shortcuts: &[Shortcut]) {
    engine.clear_shortcuts();
    for sc in shortcuts {
        engine.add_shortcut(sc.trigger.clone(), sc.expansion.clone());
    }
}
