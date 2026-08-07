//! The persisted settings document. Field names and defaults are the on-disk
//! contract: every `rename_all`/`default`/`skip_serializing_if` below is what an
//! existing `settings.json` was written with, so none of them may change casually.

use serde::{Deserialize, Serialize};

use super::{FlipHotkey, Hotkey, KeyCombo, Method, ToneStyle};

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ExcludedApp {
    pub id: String,
    pub name: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Shortcut {
    pub trigger: String,
    pub expansion: String,
}

#[derive(Debug, Clone, PartialEq, Eq, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Settings {
    pub method: Method,
    #[serde(default)]
    pub tone_style: ToneStyle,
    pub enabled: bool,
    pub smart_restore: bool,
    pub eager_restore: bool,
    #[serde(default)]
    pub spell_check: bool,
    #[serde(default)]
    pub auto_capitalize: bool,
    pub toggle_hotkey: Hotkey,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub toggle_combo: Option<KeyCombo>,
    #[serde(default)]
    pub flip_hotkey: FlipHotkey,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub flip_combo: Option<KeyCombo>,
    pub launch_at_login: bool,
    pub has_completed_onboarding: bool,
    #[serde(default)]
    pub excluded_apps: Vec<ExcludedApp>,
    #[serde(default)]
    pub shortcuts: Vec<Shortcut>,
}

impl Default for Settings {
    fn default() -> Self {
        Self {
            method: Method::Vni,
            tone_style: ToneStyle::Traditional,
            enabled: true,
            smart_restore: true,
            eager_restore: true,
            spell_check: false,
            auto_capitalize: false,
            toggle_hotkey: Hotkey::CtrlBacktick,
            toggle_combo: None,
            flip_hotkey: FlipHotkey::Off,
            flip_combo: None,
            launch_at_login: false,
            has_completed_onboarding: false,
            excluded_apps: Vec::new(),
            shortcuts: Vec::new(),
        }
    }
}

#[cfg(test)]
mod tests;
