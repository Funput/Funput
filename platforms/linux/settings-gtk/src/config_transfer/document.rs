use serde::{Deserialize, Serialize};

pub(super) const SCHEMA_ID: &str = "app.funput.config";
pub(super) const CURRENT_VERSION: u32 = 1;

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(super) struct ConfigDocument {
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

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(super) struct Source {
    pub platform: String,
    pub app_version: String,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(super) struct Preferences {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input_method: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub tone_style: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub smart_english_restore: Option<bool>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub eager_restore: Option<bool>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub spell_check: Option<bool>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub auto_capitalize: Option<bool>,
}

#[derive(Serialize, Deserialize)]
pub(super) struct PortableShortcut {
    pub trigger: String,
    pub expansion: String,
}

#[derive(Serialize, Deserialize)]
pub(super) struct Platform {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub linux: Option<LinuxBlock>,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub(super) struct LinuxBlock {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub toggle_hotkey: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub flip_hotkey: Option<String>,
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub non_preedit: Option<bool>,
}

#[derive(Default)]
pub struct ImportSummary {
    pub shortcuts_added: usize,
    pub shortcuts_updated: usize,
    pub applied_platform: bool,
    pub newer_version: bool,
}
