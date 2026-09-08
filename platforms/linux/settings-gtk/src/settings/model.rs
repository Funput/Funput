use serde::{Deserialize, Serialize};

use super::{FlipHotkey, Hotkey, Method, Shortcut, ToneStyle};

#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Settings {
    pub method: Method,
    #[serde(default = "legacy_tone_style_default")]
    pub tone_style: ToneStyle,
    pub enabled: bool,
    pub smart_restore: bool,
    pub eager_restore: bool,
    #[serde(default)]
    pub spell_check: bool,
    #[serde(default)]
    pub auto_capitalize: bool,
    /// "Gõ thẳng, không gạch chân", shown on Tổng quan. `default = "on"`, not a bare
    /// `#[serde(default)]`: that reads a keyless document as false and saves it back.
    #[serde(default = "on")]
    pub non_preedit: bool,
    pub toggle_hotkey: Hotkey,
    #[serde(default)]
    pub flip_hotkey: FlipHotkey,
    /// Hidden from Settings UI; kept so wholesale `save()` does not drop the key.
    pub launch_at_login: bool,
    pub has_completed_onboarding: bool,
    #[serde(default)]
    pub shortcuts: Vec<Shortcut>,
    /// Whether the table above expands at all. The rows stay stored either way.
    #[serde(default = "on")]
    pub shortcuts_enabled: bool,
    /// Whether a trigger matches however it was capitalized, with the expansion
    /// re-cased to match. Defaults to **on**, including for a file written before
    /// the key existed — same rule as `shortcuts_enabled`.
    #[serde(default = "on")]
    pub shortcut_smart_case: bool,
    /// Whether the table also expands while Vietnamese is off.
    #[serde(default = "on")]
    pub shortcuts_in_english: bool,
}

fn on() -> bool {
    true
}

fn legacy_tone_style_default() -> ToneStyle {
    ToneStyle::Traditional
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            method: Method::Vni,
            tone_style: ToneStyle::Modern,
            enabled: true,
            smart_restore: true,
            eager_restore: true,
            spell_check: false,
            auto_capitalize: false,
            non_preedit: true,
            toggle_hotkey: Hotkey::CtrlBacktick,
            flip_hotkey: FlipHotkey::Off,
            launch_at_login: false,
            has_completed_onboarding: false,
            shortcuts: Vec::new(),
            shortcuts_enabled: true,
            shortcut_smart_case: true,
            shortcuts_in_english: true,
        }
    }
}
