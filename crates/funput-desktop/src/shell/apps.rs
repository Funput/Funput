//! Which app gets Vietnamese.
//!
//! Three sources decide, and their priority is the whole feature: a manual toggle
//! the user just made, a manual toggle they made earlier in this app, then the
//! exclusion list. The awkward part is that both places a user can toggle from —
//! the tray and the Settings window — *steal foreground themselves*, so the app
//! the choice is meant for is not focused when the choice is made. That is what
//! `pending_override` exists for.

use funput_config::ExcludedApp;

use super::{RECENT_CAP, ShellState};

impl ShellState {
    /// Flip VI/EN from the tray; returns the new state. The tray click steals
    /// foreground, so the choice is parked and bound to the next app the user
    /// focuses — otherwise the per-app auto-switch would revert it the instant
    /// focus returns to a non-excluded app.
    pub fn toggle_enabled(&mut self) -> bool {
        let on = !self.settings.enabled;
        self.set_enabled_state(on);
        self.pending_override = Some(on);
        self.save();
        on
    }

    /// Flip VI/EN from the keyboard hotkey; returns the new state. Unlike the tray,
    /// the hotkey fires while the target app is focused, so the choice binds to
    /// that app immediately and clears any stale pending override.
    pub fn toggle_enabled_hotkey(&mut self) -> bool {
        let on = !self.settings.enabled;
        self.set_enabled_state(on);
        if let Some(app) = self.recent.first() {
            self.overrides.insert(app.id.clone(), on);
        }
        self.pending_override = None;
        self.save();
        on
    }

    /// Record the just-focused app for the "recent apps" picker (deduped,
    /// most-recent-first, capped). No-op for empty ids.
    pub fn note_foreground(&mut self, id: String, name: String) {
        if id.is_empty() {
            return;
        }
        self.recent.retain(|a| a.id != id);
        self.recent.insert(0, ExcludedApp { id, name });
        self.recent.truncate(RECENT_CAP);
    }

    /// Decide VI/EN for the newly-focused app, in priority order:
    ///
    /// 1. A pending manual toggle (from the tray / Settings, which steal
    ///    foreground) binds to this app — the user's choice lands on the app they
    ///    return to.
    /// 2. A remembered manual override for this app wins over the list default, so
    ///    a prior manual toggle survives leaving and re-focusing the app.
    /// 3. Otherwise the exclusion-list default: excluded apps → English, every
    ///    other app → Vietnamese. No-op when the list is empty, so users who don't
    ///    use the feature keep a plain global toggle.
    ///
    /// Returns `Some(on)` when it flipped VI/EN (so the caller can refresh its
    /// tray), `None` when nothing changed.
    pub fn apply_for_app(&mut self, id: &str) -> Option<bool> {
        let target = if let Some(on) = self.pending_override.take() {
            self.overrides.insert(id.to_string(), on);
            on
        } else if let Some(&on) = self.overrides.get(id) {
            on
        } else if self.settings.excluded_apps.is_empty() {
            return None;
        } else {
            !self.settings.excluded_apps.iter().any(|a| a.id == id)
        };

        if self.settings.enabled == target {
            return None;
        }
        self.set_enabled_state(target);
        self.save();
        Some(target)
    }

    /// Seed a UI process with the background process's runtime-only recent list.
    pub fn set_recent_apps(&mut self, apps: Vec<ExcludedApp>) {
        self.recent = apps;
    }

    pub fn add_excluded_app(&mut self, app: ExcludedApp) {
        if !self.settings.excluded_apps.iter().any(|a| a.id == app.id) {
            self.settings.excluded_apps.push(app);
            self.save();
        }
    }

    pub fn remove_excluded_app(&mut self, id: &str) {
        let before = self.settings.excluded_apps.len();
        self.settings.excluded_apps.retain(|a| a.id != id);
        if self.settings.excluded_apps.len() != before {
            self.save();
        }
    }
}
