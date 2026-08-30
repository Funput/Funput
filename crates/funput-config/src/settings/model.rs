//! The persisted settings document. Field names and defaults are the on-disk
//! contract: every `rename_all`/`default`/`skip_serializing_if` below is what an
//! existing `settings.json` was written with, so none of them may change casually.

use std::collections::BTreeMap;

use serde::{Deserialize, Serialize};

use super::{FlipHotkey, Hotkey, KeyCombo, Method, ToneStyle};

/// One entry of the removed "always English" list. Kept only so a `settings.json`
/// or an exported config written before the per-app memory existed still decodes —
/// each `id` is migrated into [`Settings::app_language_memory`] as English.
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
    /// Per-app VI/EN memory, keyed by the platform's app id (on Windows, the
    /// lowercased exe name). An app is here only because the user toggled inside
    /// it; an absent id means "no opinion" and the auto-switch leaves the global
    /// state alone. A `BTreeMap` so the pretty-printed file has a stable key
    /// order — this is rewritten on every focus change that flips VI/EN.
    #[serde(default)]
    pub app_language_memory: BTreeMap<String, bool>,
    /// **Legacy, read-only.** Drained into `app_language_memory` by
    /// [`Settings::load_from`], so it is always empty afterwards, and never
    /// written back (`skip_serializing`).
    #[serde(default, skip_serializing)]
    pub excluded_apps: Vec<ExcludedApp>,
    #[serde(default)]
    pub shortcuts: Vec<Shortcut>,
    /// Whether the gõ tắt table expands. Defaults to **on**, including for a file
    /// written before this field existed — those users have a working table and
    /// would otherwise find it silently dead after an update.
    #[serde(default = "shortcuts_enabled_default")]
    pub shortcuts_enabled: bool,
    /// Whether a gõ tắt trigger matches regardless of how it was capitalized, with
    /// the expansion re-cased to match. Defaults to **on**, including for a file
    /// written before this field existed — that is how gõ tắt has always behaved,
    /// and an update must not silently change what a table expands to.
    #[serde(default = "shortcut_smart_case_default")]
    pub shortcut_smart_case: bool,
    /// Whether focusing a keyboard layout Vietnamese cannot be typed on — a CJK
    /// IME, or a non-Latin script — suspends Vietnamese for as long as it is
    /// active. Defaults to **on**, including for a file written before this field
    /// existed: composing into a Japanese IME corrupts its text, and the users who
    /// already hit that are exactly the ones who would never find the switch.
    /// The suspension itself is live state, not settings — see `funput_desktop`.
    #[serde(default = "auto_english_on_foreign_layout_default")]
    pub auto_english_on_foreign_layout: bool,
}

fn shortcuts_enabled_default() -> bool {
    true
}

fn shortcut_smart_case_default() -> bool {
    true
}

fn auto_english_on_foreign_layout_default() -> bool {
    true
}

impl Settings {
    /// Fold legacy "always English" app ids into the per-app memory. An id the
    /// user has since toggled keeps its remembered choice — existing wins, the
    /// same rule import uses.
    pub(crate) fn remember_as_english(&mut self, ids: impl IntoIterator<Item = String>) {
        for id in ids {
            if id.is_empty() {
                continue;
            }
            self.app_language_memory.entry(id).or_insert(false);
        }
    }
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
            app_language_memory: BTreeMap::new(),
            excluded_apps: Vec::new(),
            shortcuts: Vec::new(),
            shortcuts_enabled: true,
            shortcut_smart_case: true,
            auto_english_on_foreign_layout: true,
        }
    }
}

#[cfg(test)]
mod tests;
