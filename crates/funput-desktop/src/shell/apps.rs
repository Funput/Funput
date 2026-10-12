//! Which app gets Vietnamese.
//!
//! Funput does not decide for the user; it remembers what the user decided. An
//! app gets a VI/EN state only once a manual toggle happened inside it, and that
//! choice then survives leaving, re-focusing, and restarting. An app nobody has
//! toggled in has no opinion attached, so focusing it changes nothing.
//!
//! **The latest choice wins, and no pin is made behind the user's back.**
//!
//! - The hotkey is pressed inside the app it means, so it pins that app.
//! - The Control Center and the Settings window are Funput's own windows, opened
//!   with no app in front. They move the global default and create no pin. But if
//!   the app the user left to open them already has one, they move that pin too.
//!   Otherwise coming back to it would replay the old pin, and the choice the user
//!   just made would look undone.
//!
//! Those two used to park their choice and bind it to whichever app took focus next
//! — which, after a tray click, was the taskbar: an app the user never mentioned,
//! pinned invisibly, since nothing on Windows displays this map. The app *left*
//! is known before the flyout opens, and only a pin that already exists is moved.
//!
//! All of which the user can switch off (`Settings::app_language_memory_enabled`),
//! and then this map is history: nothing is written, nothing is replayed, and every
//! app follows the global VI/EN state. The entries stay, so switching it back on
//! hands the pins back rather than starting over.

use super::ShellState;

impl ShellState {
    /// Remember the user's VI/EN choice for `id`, reporting whether that actually
    /// changed the map. Two things are turned away: an empty id, because a window
    /// we could not resolve to an executable must not claim an entry, and every id
    /// while the memory is switched off. The caller persists, because every caller
    /// has its own reason to write and would otherwise write twice; the pin is also
    /// queued so [`Self::save_settings`] can merge it onto a fresh read of the file.
    pub(super) fn remember(&mut self, id: &str, on: bool) -> bool {
        if id.is_empty() || !self.settings.app_language_memory_enabled {
            return false;
        }
        self.unsaved_pins.push((id.to_string(), on));
        self.settings.app_language_memory.insert(id.to_string(), on) != Some(on)
    }

    /// Flip VI/EN from the keyboard hotkey; returns the new state. Unlike the tray,
    /// the hotkey fires while the target app is focused — so it is the one toggle
    /// that means "just here", and the only thing that creates a pin.
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

    /// Record the app that just took focus, so a hotkey toggle knows what to bind
    /// to. An empty id is not an app, and is handled like [`Self::clear_foreground`].
    pub fn note_foreground(&mut self, id: String) {
        if !id.is_empty() {
            self.last_app = Some(id.clone());
        }
        self.foreground = (!id.is_empty()).then_some(id);
    }

    /// Focus moved somewhere that is not an app the user types in: the taskbar, the
    /// desktop, a Funput window, or a window whose program could not be resolved. A
    /// hotkey pressed there is a global switch and pins nothing — leaving the last
    /// app on record instead let it re-pin the app the user had just *left*.
    pub fn clear_foreground(&mut self) {
        self.foreground = None;
    }

    /// A VI/EN flip arrived from the Control Center or the Settings window: carry
    /// it to the pin of the app the user left to make it, if that app has one.
    /// Never creates a pin — see the module docs.
    pub(super) fn carry_flip_to_last_app(&mut self) {
        let on = self.settings.enabled;
        let Some(id) = self.last_app.clone() else {
            return;
        };
        let pinned = self.settings.app_language_memory.get(&id).copied();
        if pinned.is_some_and(|was| was != on) && self.remember(&id, on) {
            self.save_settings();
        }
    }

    /// Replay the choice remembered for the newly-focused app, if it has one.
    ///
    /// An app with none is left alone: it inherits whatever state the previous app
    /// had, which is what "we only remember what you told us" means — and it is why
    /// a global toggle reaches every app nobody has pinned without this being told
    /// anything.
    ///
    /// **Reads the map, never writes it.** A pin changes only when the user makes a
    /// choice: [`Self::toggle_enabled_hotkey`] and [`Self::carry_flip_to_last_app`].
    ///
    /// Returns `Some(on)` when it flipped VI/EN (so the caller can refresh its
    /// tray), `None` when nothing changed — including every app once the user has
    /// switched the memory off, which is what makes that switch felt: the map is
    /// still on disk, but nothing consults it.
    pub fn apply_for_app(&mut self, id: &str) -> Option<bool> {
        if !self.settings.app_language_memory_enabled {
            return None;
        }
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
}
