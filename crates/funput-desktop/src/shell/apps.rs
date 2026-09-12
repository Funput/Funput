//! Which app gets Vietnamese.
//!
//! Funput does not decide for the user; it remembers what the user decided. An
//! app gets a VI/EN state only once a manual toggle happened inside it, and that
//! choice then survives leaving, re-focusing, and restarting. An app nobody has
//! toggled in has no opinion attached, so focusing it changes nothing.
//!
//! **The surface a toggle came from says its scope**, which is what keeps the rule
//! above true. The hotkey is pressed inside the app it means, so it pins that app.
//! The tray flyout and the Settings window are Funput's own windows, opened from the
//! tray with no app in sight, so they move the global default and pin nothing.
//!
//! Those two used to park their choice and bind it to whichever app took focus next
//! — the flyout steals foreground, so there was no app to bind to at the time. That
//! made turning Vietnamese off in the tray write a permanent entry here for whatever
//! the user clicked into afterwards: an app they never mentioned, pinned invisibly,
//! since nothing on Windows displays this map, and still overruling the global switch
//! long after. A global surface now stays global.

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
    /// the hotkey fires while the target app is focused — so it is the one toggle
    /// that means "just here", and the only thing that writes this map.
    ///
    /// In memory only. The caller persists with [`Self::save_settings`] once it is
    /// somewhere it can afford to block: on Windows this runs inside the low-level
    /// keyboard hook, and a hook callback that overruns `LowLevelHooksTimeout` is
    /// silently unhooked by the OS — a file write cannot promise to beat it.
    pub fn toggle_enabled_hotkey(&mut self) -> bool {
        // Against what is *running*: with Vietnamese suspended by a foreign layout
        // the setting still says "on", and toggling that off would look dead.
        let on = !self.effective_enabled();
        if on && self.layout_suspended {
            // They want Vietnamese on this keyboard after all — Funput has made its
            // case, so stop suspending until they move to a different layout.
            self.layout_suspended = false;
            self.layout_override = self.last_layout;
        }
        self.set_enabled_state(on);
        if let Some(id) = self.foreground_id().map(str::to_string) {
            self.remember(&id, on);
        }
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

    /// Replay the choice remembered for the newly-focused app, if it has one.
    ///
    /// An app with none is left alone: it inherits whatever state the previous app
    /// had, which is what "we only remember what you told us" means — and it is why
    /// a global toggle reaches every app nobody has pinned without this being told
    /// anything.
    ///
    /// **Reads the map, never writes it.** Only [`Self::toggle_enabled_hotkey`]
    /// writes, because only the hotkey is pressed inside the app it is about.
    ///
    /// Returns `Some(on)` when it flipped VI/EN (so the caller can refresh its
    /// tray), `None` when nothing changed.
    pub fn apply_for_app(&mut self, id: &str) -> Option<bool> {
        let target = *self.settings.app_language_memory.get(id)?;
        let before = self.effective_enabled();
        if self.settings.enabled != target {
            self.set_enabled_state(target);
            self.save();
        }
        // Reported against what is running: an app remembered as Vietnamese does
        // not light the tray up while a foreign layout has it suspended.
        let after = self.effective_enabled();
        (after != before).then_some(after)
    }

    /// Decide VI/EN for the keyboard layout under the caret, from its handle.
    ///
    /// Unlike the per-app memory this is a rule, not a recollection: Vietnamese
    /// cannot be typed on a CJK IME or a non-Latin layout at all (see
    /// [`crate::is_foreign_layout`]). And it is the *last* word — the platform asks
    /// it after [`Self::apply_for_app`], so it outranks a remembered choice.
    ///
    /// Nothing is persisted: the suspension describes the keyboard in front of the
    /// user, would be stale by the next launch, and layouts change far too often to
    /// spend a file write on. Returns `Some(on)` only when Vietnamese actually
    /// started or stopped running, so the caller can refresh its tray.
    pub fn apply_for_layout(&mut self, layout: u32) -> Option<bool> {
        if layout == self.last_layout {
            return None;
        }
        self.last_layout = layout;
        // An override earns its keep only on the layout it was granted for.
        if layout != self.layout_override {
            self.layout_override = 0;
        }
        let suspend = self.settings.auto_english_on_foreign_layout
            && layout != self.layout_override
            && crate::is_foreign_layout(layout);
        if suspend == self.layout_suspended {
            return None;
        }
        let before = self.effective_enabled();
        self.layout_suspended = suspend;
        let after = self.effective_enabled();
        if after == before {
            return None; // the user is in English anyway; nothing to suspend
        }
        self.engine.set_enabled(after);
        // Someone else's composition owns the caret now (or did), so the shadow of
        // what Funput typed no longer describes the text sitting in front of it.
        self.reset_composition();
        Some(after)
    }
}
