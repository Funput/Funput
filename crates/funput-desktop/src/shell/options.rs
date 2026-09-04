//! The individual setting writes the Settings UI drives. Each persists.

use funput_config::{FlipHotkey, Hotkey, KeyCombo, Method, ToneStyle};
use funput_core::{InputMethod, ToneStyle as CoreToneStyle};

use super::ShellState;

impl ShellState {
    pub fn set_method(&mut self, method: InputMethod) {
        // Spelled out rather than routed through `update_config` so the extra
        // composition reset happens under the *same* lock the caller holds: the
        // hook thread must never see the new method with a buffer still composed
        // under the old grammar.
        self.settings.method = Method::from_core(method);
        self.sync_engine_config();
        self.reset_composition();
        self.save();
    }

    pub fn set_tone_style(&mut self, style: CoreToneStyle) {
        self.update_config(|s| s.tone_style = ToneStyle::from_core(style));
    }

    pub fn set_smart_restore(&mut self, on: bool) {
        self.update_config(|s| s.smart_restore = on);
    }

    pub fn set_eager_restore(&mut self, on: bool) {
        self.update_config(|s| s.eager_restore = on);
    }

    pub fn set_spell_check(&mut self, on: bool) {
        self.update_config(|s| s.spell_check = on);
    }

    pub fn set_auto_capitalize(&mut self, on: bool) {
        self.update_config(|s| s.auto_capitalize = on);
    }

    /// Turn gõ tắt expansion on or off. The table itself is left alone, so the rows
    /// are still there — and still editable — while it is off.
    pub fn set_shortcuts_enabled(&mut self, on: bool) {
        self.update_config(|s| s.shortcuts_enabled = on);
    }

    /// Turn smart-case trigger matching on or off. Off, only the trigger as stored
    /// expands and the expansion is emitted verbatim — which is what a table
    /// holding both `vn` and `VN` needs.
    pub fn set_shortcut_smart_case(&mut self, on: bool) {
        self.update_config(|s| s.shortcut_smart_case = on);
    }

    /// Turn the foreign-layout auto-switch on or off. Re-judges the current layout
    /// straight away, so turning it off lifts a suspension already in place instead
    /// of leaving Vietnamese off until the user next changes keyboards.
    pub fn set_auto_english_on_foreign_layout(&mut self, on: bool) {
        self.update_config(|s| s.auto_english_on_foreign_layout = on);
        self.redecide_layout();
    }

    /// Picking a preset also clears any recorded custom combo — the two are
    /// alternatives, and the combo (when present) always wins in the hook.
    pub fn set_toggle_hotkey(&mut self, hotkey: Hotkey) {
        self.settings.toggle_hotkey = hotkey;
        self.settings.toggle_combo = None;
        self.save();
    }

    pub fn set_toggle_combo(&mut self, combo: KeyCombo) {
        self.settings.toggle_combo = Some(combo);
        self.save();
    }

    pub fn set_flip_hotkey(&mut self, hotkey: FlipHotkey) {
        self.settings.flip_hotkey = hotkey;
        self.settings.flip_combo = None;
        self.save();
    }

    pub fn set_flip_combo(&mut self, combo: KeyCombo) {
        self.settings.flip_combo = Some(combo);
        self.save();
    }

    /// Persist the launch-at-login preference. The OS side effect (an autostart
    /// registry entry, a plist) belongs to the platform, which owns that
    /// integration.
    pub fn set_launch_at_login(&mut self, on: bool) {
        self.settings.launch_at_login = on;
        self.save();
    }

    pub fn complete_onboarding(&mut self) {
        self.settings.has_completed_onboarding = true;
        self.save();
    }

    /// Flip VI/EN from a Funput window (the Settings pane or the tray flyout).
    /// That window holds focus while this runs, so the choice is parked and bound
    /// to the next app the user returns to.
    pub fn set_enabled(&mut self, on: bool) {
        self.set_enabled_state(on);
        self.pending_override = Some(on);
        self.save();
    }
}
