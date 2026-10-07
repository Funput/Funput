//! One settings file, written by more than one process.
//!
//! On Windows the keyboard hook runs in the background process, while Settings and
//! the Control Center are processes of their own, and all three read and write the
//! same `settings.json`. Each holds a whole `Settings` in memory and saves it whole,
//! so a write from a stale copy undoes whatever another process changed since that
//! copy was read.
//!
//! The fields split by owner. VI/EN and the per-app memory change under the hook,
//! from a keystroke; everything else is edited in a UI. So before writing, each side
//! takes the other side's fields from the file instead of its own stale copy — see
//! [`ShellState::refresh_hook_state`] and [`ShellState::save_settings`].

use funput_config::Settings;

use crate::shell::ShellState;

impl ShellState {
    /// Reload settings written by another process (the Settings window and the
    /// Control Center each run as their own). The focused app is untouched; only
    /// persisted state and the live engine are refreshed. Returns whether anything
    /// actually changed, so the caller can skip refreshing its UI.
    pub fn reload_settings(&mut self) -> bool {
        // A VI/EN flip made in one of those windows arrives as a plain settings
        // field, and `adopt` pushes it to the engine. Nothing else is owed: it was a
        // global choice, so it needs no app to land on.
        let loaded = self.read_settings();
        self.adopt(loaded)
    }

    /// Write down what [`Self::toggle_enabled_hotkey`] changed. Split out because
    /// the hook cannot afford a file write; the platform calls it one message later.
    ///
    /// The file is read fresh and only what the hotkey decided — VI/EN, and the pins
    /// it made — is laid over it. Writing this process's copy instead would put back
    /// every option the Settings window changed since this process last reloaded.
    pub fn save_settings(&mut self) {
        let pins = std::mem::take(&mut self.unsaved_pins);
        if !self.has_settings_on_disk() {
            self.save(); // nobody else has written anything to merge with
            return;
        }
        let mut fresh = self.read_settings();
        fresh.enabled = self.settings.enabled;
        if fresh.app_language_memory_enabled {
            fresh.app_language_memory.extend(pins);
        }
        self.adopt(fresh);
        self.save();
    }

    /// Take VI/EN and the per-app memory from the file — the two fields the hook
    /// writes — leaving everything else as this process has it. A UI process calls
    /// this right before each write.
    ///
    /// Without it, Settings left open saved the VI/EN state and app pins from when
    /// it opened with every option the user changed, so an app pinned to English by
    /// the hotkey came back as Vietnamese. Nothing else is taken: the rest is the
    /// UI's to edit, including gõ tắt drafts that are deliberately kept off disk.
    pub fn refresh_hook_state(&mut self) {
        if !self.has_settings_on_disk() {
            return;
        }
        let disk = self.read_settings();
        self.settings.app_language_memory = disk.app_language_memory;
        if self.settings.enabled != disk.enabled {
            self.set_enabled_state(disk.enabled);
        }
    }

    /// Take `loaded` as the settings, pushing it to the engine if it differs.
    /// Returns whether it did.
    fn adopt(&mut self, loaded: Settings) -> bool {
        if self.settings == loaded {
            return false;
        }
        self.settings = loaded;
        self.apply_settings();
        // The foreign-layout switch may have been the thing that changed, and the
        // layout it applies to has not moved — re-judge it rather than waiting.
        self.redecide_layout();
        true
    }

    /// Whether there is a file another process may have written. Without one, a
    /// read returns defaults, and merging with those would only lose this state.
    fn has_settings_on_disk(&self) -> bool {
        self.settings_file
            .as_deref()
            .is_some_and(|path| path.exists())
    }
}
