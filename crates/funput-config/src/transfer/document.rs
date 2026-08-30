//! The wire shapes of the interchange file, specified in
//! `platforms/CONFIG_FORMAT.md`.
//!
//! Every field is optional so a partial file, an older file, or one written by a
//! newer version still decodes; import then applies only what is present. Unknown
//! keys are skipped by serde, which is what lets a macOS export deliver its
//! portable half to a Windows machine.

use std::collections::BTreeMap;

use serde::{Deserialize, Serialize};

use crate::settings::{ExcludedApp, KeyCombo};

/// Value of the `schema` field. A file carrying anything else is not ours.
pub const SCHEMA_ID: &str = "app.funput.config";

/// Format version this build writes. A file numbered higher still imports, on a
/// best-effort basis — see [`super::ImportSummary::newer_version`].
pub const CURRENT_VERSION: u32 = 1;

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConfigDocument {
    pub schema: String,
    #[serde(default)]
    pub version: u32,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub exported_at: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub source: Option<Source>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub preferences: Option<Preferences>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub shortcuts: Option<Vec<PortableShortcut>>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub platform: Option<Platform>,
}

/// Which app wrote the file. Metadata only — never applied on import.
///
/// The shell supplies this: `app_version` must be the *application's* version, not
/// this crate's, so it cannot be filled in from `CARGO_PKG_VERSION` here.
#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Source {
    pub platform: String,
    pub app_version: String,
}

/// Portable typing preferences — meaningful on every platform.
#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Preferences {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input_method: Option<String>, // "telex" | "telex_advanced" | "vni"
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tone_style: Option<String>, // "traditional" | "modern"
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub smart_english_restore: Option<bool>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub eager_restore: Option<bool>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub spell_check: Option<bool>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub auto_capitalize: Option<bool>,
    /// Whether the gõ tắt rows carried in `shortcuts` actually expand. Portable
    /// because it is a typing preference, not a per-OS one.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub shortcuts_enabled: Option<bool>,
    /// Whether a gõ tắt trigger matches regardless of capitalization, with the
    /// expansion re-cased to match. Portable for the same reason.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub shortcut_smart_case: Option<bool>,
}

/// Comparable and printable because it is also what [`crate::unikey`] parses a
/// UniKey macro table into, and those rows are asserted on directly in tests.
#[derive(Debug, PartialEq, Eq, Serialize, Deserialize)]
pub struct PortableShortcut {
    pub trigger: String,
    pub expansion: String,
}

/// Per-OS data, applied only by the platform it names. Other platforms' blocks
/// (`macos`, and `linux` once it adopts this crate) are skipped by serde, so a
/// foreign file still imports its portable parts.
#[derive(Serialize, Deserialize)]
pub struct Platform {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub windows: Option<WindowsBlock>,
}

/// Windows identifies apps by exe name and supports both hotkey presets and
/// user-recorded virtual-key combos, so its block is richer than the macOS one.
#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct WindowsBlock {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub toggle_hotkey: Option<String>, // Hotkey preset id
    /// User-recorded toggle combo; overrides the preset when present.
    #[serde(default)]
    pub toggle_combo: Option<KeyCombo>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub flip_hotkey: Option<String>, // FlipHotkey preset id
    /// User-recorded flip combo; overrides the preset when present.
    #[serde(default)]
    pub flip_combo: Option<KeyCombo>,
    /// Per-app VI/EN memory, keyed by lowercased exe name. Merged by key on
    /// import — an app already remembered locally keeps its choice.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub app_language_memory: Option<BTreeMap<String, bool>>,
    /// **Legacy, read-only.** The removed "always English" list, still decoded
    /// from older exports so their ids migrate into `app_language_memory` as
    /// English. Never written.
    #[serde(default, skip_serializing)]
    pub excluded_apps: Option<Vec<ExcludedApp>>,
}
