//! Which app gets Vietnamese.
//!
//! Funput does not decide for the user; it remembers what the user decided. An
//! app gets a VI/EN state only once a manual toggle happened inside it, and that
//! choice then survives leaving, re-focusing, and restarting. An app nobody has
//! toggled in has no opinion attached, so focusing it changes nothing.
//!
//! The awkward part is that both places a user can toggle from — the tray flyout
//! and the Settings window — *steal foreground themselves*, so the app the choice
//! is meant for is not focused when the choice is made. That is what
//! `pending_override` exists for.

use super::ShellState;

impl ShellState {
    /// Remember the user's VI/EN choice for `id`, reporting whether that actually
    /// changed the map. Empty ids are ignored — a window we could not resolve to
    /// an executable must not claim an entry. The caller persists, because every
    /// caller has its own reason to write and would otherwise write twice.
    pub(super) fn remember(&mut self, id: &str, on: bool) -> bool {
        if id.is_empty() {
            return false;
        }
        self.settings.app_language_memory.insert(id.to_string(), on) != Some(on)
    }

    /// Flip VI/EN from the keyboard hotkey; returns the new state. Unlike the tray,
    /// the hotkey fires while the target app is focused, so the choice binds to
    /// that app immediately and clears any stale pending override.
    ///
    /// In memory only. The caller persists with [`Self::save_settings`] once it is
    /// somewhere it can afford to block: on Windows this runs inside the low-level
    /// keyboard hook, and a hook callback that overruns `LowLevelHooksTimeout` is
    /// silently unhooked by the OS — a file write cannot promise to beat it.
    pub fn toggle_enabled_hotkey(&mut self) -> bool {
        let on = !self.settings.enabled;
        self.set_enabled_state(on);
        if let Some(id) = self.foreground_id().map(str::to_string) {
            self.remember(&id, on);
        }
        self.pending_override = None;
        on
    }

    /// Write down what [`Self::toggle_enabled_hotkey`] changed. Split out because
    /// it is the one write in this file whose caller may have to postpone it; the
    /// rest happen where blocking is free.
    pub fn save_settings(&self) {
        self.save();
    }

    /// Record the app that just took focus, so a hotkey toggle knows what to bind
    /// to. No-op for empty ids — a window we could not resolve leaves the previous
    /// app standing rather than blanking it.
    pub fn note_foreground(&mut self, id: String) {
        if id.is_empty() {
            return;
        }
        self.foreground = Some(id);
    }

    /// Decide VI/EN for the newly-focused app:
    ///
    /// 1. A pending manual toggle (from the tray / Settings, which steal
    ///    foreground) binds to this app — the user's choice lands on the app they
    ///    return to, and is remembered there from now on.
    /// 2. Otherwise the choice remembered for this app, if it has one.
    ///
    /// An app with neither is left alone: it inherits whatever state the previous
    /// app had, which is what "we only remember what you told us" means.
    ///
    /// Returns `Some(on)` when it flipped VI/EN (so the caller can refresh its
    /// tray), `None` when nothing changed.
    pub fn apply_for_app(&mut self, id: &str) -> Option<bool> {
        let mut remembered = false;
        let target = if let Some(on) = self.pending_override.take() {
            remembered = self.remember(id, on);
            on
        } else {
            *self.settings.app_language_memory.get(id)?
        };

        // The map is persisted now, so a choice that binds without flipping the
        // state still has to reach disk — returning early here would drop it.
        let flipped = self.settings.enabled != target;
        if flipped {
            self.set_enabled_state(target);
        }
        if flipped || remembered {
            self.save();
        }
        flipped.then_some(target)
    }
}
