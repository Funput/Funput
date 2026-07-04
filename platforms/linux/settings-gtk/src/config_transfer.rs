//! Cross-platform config interchange (Export/Import) — see `platforms/CONFIG_FORMAT.md`.
//!
//! Pure `serde`/`serde_json` (no GTK deps) so it unit-tests on any host. Mirrors the
//! Windows implementation; `preferences` + `shortcuts` are portable, Linux-only data
//! (hotkey preset, excluded WM_CLASS list) lives under `platform.linux`. Other
//! platforms' blocks (`macos`, `windows`) are ignored on import — serde skips unknown
//! keys — so files exported elsewhere still import their portable parts here.

use std::fs;
use std::path::Path;
use std::time::{SystemTime, UNIX_EPOCH};

use serde::{Deserialize, Serialize};

use crate::settings::{ExcludedApp, FlipHotkey, Hotkey, Method, Settings, Shortcut, ToneStyle};

const SCHEMA_ID: &str = "app.funput.config";
const CURRENT_VERSION: u32 = 1;

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

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Source {
    pub platform: String,
    pub app_version: String,
}

/// Portable typing preferences. Every field is optional so a partial or future file
/// still decodes; on import we overwrite only the ones present.
#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct Preferences {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub input_method: Option<String>, // "telex" | "vni"
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
}

#[derive(Serialize, Deserialize)]
pub struct PortableShortcut {
    pub trigger: String,
    pub expansion: String,
}

/// Only the Linux block is modelled. Unknown platform keys (`macos`, `windows`) are
/// skipped by serde, so cross-platform files still import their portable parts.
#[derive(Serialize, Deserialize)]
pub struct Platform {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub linux: Option<LinuxBlock>,
}

#[derive(Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct LinuxBlock {
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub toggle_hotkey: Option<String>, // Hotkey preset id
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub flip_hotkey: Option<String>, // FlipHotkey preset id
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub excluded_apps: Option<Vec<ExcludedApp>>,
}

/// What an import changed, for the confirmation shown to the user.
#[derive(Default)]
pub struct ImportSummary {
    pub shortcuts_added: usize,
    pub shortcuts_updated: usize,
    pub applied_platform: bool,
    pub newer_version: bool,
}

/// A user-facing failure while importing a config file.
#[derive(Debug)]
pub enum ConfigError {
    Unreadable,   // not JSON / decode failed
    Unrecognized, // decoded, but not a Funput config
}

impl std::fmt::Display for ConfigError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        f.write_str(match self {
            ConfigError::Unreadable => "Không đọc được tệp cấu hình (định dạng sai hoặc tệp hỏng).",
            ConfigError::Unrecognized => "Tệp này không phải cấu hình Funput hợp lệ.",
        })
    }
}

impl std::error::Error for ConfigError {}

// The Linux enums have no `id()`/`from_id()` (the UI is index-based), so map here.
// Values match each enum's serde `rename_all`, i.e. the strings written to disk.

fn method_key(m: Method) -> &'static str {
    match m {
        Method::Telex => "telex",
        Method::Vni => "vni",
    }
}

fn method_from_key(s: &str) -> Option<Method> {
    match s {
        "telex" => Some(Method::Telex),
        "vni" => Some(Method::Vni),
        _ => None,
    }
}

fn tone_key(t: ToneStyle) -> &'static str {
    match t {
        ToneStyle::Traditional => "traditional",
        ToneStyle::Modern => "modern",
    }
}

fn tone_from_key(s: &str) -> Option<ToneStyle> {
    match s {
        "traditional" => Some(ToneStyle::Traditional),
        "modern" => Some(ToneStyle::Modern),
        _ => None,
    }
}

fn hotkey_key(h: Hotkey) -> &'static str {
    match h {
        Hotkey::CtrlBacktick => "ctrl_backtick",
        Hotkey::CtrlSpace => "ctrl_space",
        Hotkey::AltShift => "alt_shift",
    }
}

fn hotkey_from_key(s: &str) -> Option<Hotkey> {
    match s {
        "ctrl_backtick" => Some(Hotkey::CtrlBacktick),
        "ctrl_space" => Some(Hotkey::CtrlSpace),
        "alt_shift" => Some(Hotkey::AltShift),
        _ => None,
    }
}

fn flip_key(f: FlipHotkey) -> &'static str {
    match f {
        FlipHotkey::Off => "off",
        FlipHotkey::CtrlShiftZ => "ctrl_shift_z",
        FlipHotkey::CtrlShiftX => "ctrl_shift_x",
    }
}

fn flip_from_key(s: &str) -> Option<FlipHotkey> {
    match s {
        "off" => Some(FlipHotkey::Off),
        "ctrl_shift_z" => Some(FlipHotkey::CtrlShiftZ),
        "ctrl_shift_x" => Some(FlipHotkey::CtrlShiftX),
        _ => None,
    }
}

/// Snapshot the current settings into an interchange document.
pub fn to_document(s: &Settings) -> ConfigDocument {
    ConfigDocument {
        schema: SCHEMA_ID.to_string(),
        version: CURRENT_VERSION,
        exported_at: Some(iso8601_now()),
        source: Some(Source {
            platform: "linux".to_string(),
            app_version: env!("CARGO_PKG_VERSION").to_string(),
        }),
        preferences: Some(Preferences {
            input_method: Some(method_key(s.method).to_string()),
            tone_style: Some(tone_key(s.tone_style).to_string()),
            smart_english_restore: Some(s.smart_restore),
            eager_restore: Some(s.eager_restore),
            spell_check: Some(s.spell_check),
            auto_capitalize: Some(s.auto_capitalize),
        }),
        shortcuts: Some(
            s.shortcuts
                .iter()
                .map(|sc| PortableShortcut { trigger: sc.trigger.clone(), expansion: sc.expansion.clone() })
                .collect(),
        ),
        platform: Some(Platform {
            linux: Some(LinuxBlock {
                toggle_hotkey: Some(hotkey_key(s.toggle_hotkey).to_string()),
                flip_hotkey: Some(flip_key(s.flip_hotkey).to_string()),
                excluded_apps: Some(s.excluded_apps.clone()),
            }),
        }),
    }
}

/// Merge a document into `s`. Non-destructive: overwrites present fields, merges
/// shortcuts by trigger, unions excluded apps, and never deletes existing data.
pub fn apply(s: &mut Settings, doc: &ConfigDocument) -> ImportSummary {
    let mut summary = ImportSummary { newer_version: doc.version > CURRENT_VERSION, ..Default::default() };

    if let Some(prefs) = &doc.preferences {
        if let Some(v) = &prefs.input_method {
            if let Some(m) = method_from_key(v) { s.method = m; }
        }
        if let Some(v) = &prefs.tone_style {
            if let Some(t) = tone_from_key(v) { s.tone_style = t; }
        }
        if let Some(v) = prefs.smart_english_restore { s.smart_restore = v; }
        if let Some(v) = prefs.eager_restore { s.eager_restore = v; }
        if let Some(v) = prefs.spell_check { s.spell_check = v; }
        if let Some(v) = prefs.auto_capitalize { s.auto_capitalize = v; }
    }

    if let Some(incoming) = &doc.shortcuts {
        for item in incoming {
            if let Some(existing) = s.shortcuts.iter_mut().find(|sc| sc.trigger == item.trigger) {
                if existing.expansion != item.expansion {
                    existing.expansion = item.expansion.clone();
                    summary.shortcuts_updated += 1;
                }
            } else {
                s.shortcuts.push(Shortcut { trigger: item.trigger.clone(), expansion: item.expansion.clone() });
                summary.shortcuts_added += 1;
            }
        }
    }

    // Platform-specific block: apply only the one matching this OS (Linux).
    if let Some(lin) = doc.platform.as_ref().and_then(|p| p.linux.as_ref()) {
        if let Some(v) = &lin.toggle_hotkey {
            if let Some(h) = hotkey_from_key(v) { s.toggle_hotkey = h; }
        }
        if let Some(v) = &lin.flip_hotkey {
            if let Some(h) = flip_from_key(v) { s.flip_hotkey = h; }
        }
        if let Some(apps) = &lin.excluded_apps {
            for app in apps {
                if !s.excluded_apps.iter().any(|e| e.id == app.id) {
                    s.excluded_apps.push(app.clone());
                }
            }
        }
        summary.applied_platform = true;
    }

    summary
}

/// Write the current settings as a pretty-printed interchange file.
pub fn export_to(path: &Path, s: &Settings) -> std::io::Result<()> {
    let json = serde_json::to_string_pretty(&to_document(s))
        .map_err(|e| std::io::Error::new(std::io::ErrorKind::Other, e))?;
    fs::write(path, json)
}

/// Read + merge a config file into `s`. `Err` on a bad/foreign file.
pub fn import_file(path: &Path, s: &mut Settings) -> Result<ImportSummary, ConfigError> {
    let text = fs::read_to_string(path).map_err(|_| ConfigError::Unreadable)?;
    let doc: ConfigDocument = serde_json::from_str(&text).map_err(|_| ConfigError::Unreadable)?;
    if doc.schema != SCHEMA_ID {
        return Err(ConfigError::Unrecognized);
    }
    Ok(apply(s, &doc))
}

/// `yyyy-MM-dd` for the default export file name.
pub fn today_stamp() -> String {
    iso8601_now()[..10].to_string()
}

/// UTC ISO-8601 timestamp without a date crate (Howard Hinnant's days→civil algorithm).
fn iso8601_now() -> String {
    let secs = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_secs())
        .unwrap_or(0) as i64;
    let days = secs.div_euclid(86_400);
    let tod = secs.rem_euclid(86_400);
    let (hour, min, sec) = (tod / 3600, (tod % 3600) / 60, tod % 60);

    let z = days + 719_468;
    let era = if z >= 0 { z } else { z - 146_096 } / 146_097;
    let doe = z - era * 146_097;
    let yoe = (doe - doe / 1460 + doe / 36_524 - doe / 146_096) / 365;
    let year = yoe + era * 400;
    let doy = doe - (365 * yoe + yoe / 4 - yoe / 100);
    let mp = (5 * doy + 2) / 153;
    let day = doy - (153 * mp + 2) / 5 + 1;
    let month = if mp < 10 { mp + 3 } else { mp - 9 };
    let year = year + i64::from(month <= 2);

    format!("{year:04}-{month:02}-{day:02}T{hour:02}:{min:02}:{sec:02}Z")
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn round_trip_preserves_state() {
        let mut a = Settings::default();
        a.method = Method::Telex;
        a.tone_style = ToneStyle::Modern;
        a.toggle_hotkey = Hotkey::AltShift;
        a.shortcuts = vec![Shortcut { trigger: "vn".into(), expansion: "Việt Nam".into() }];
        a.excluded_apps = vec![ExcludedApp { id: "code".into(), name: "VS Code".into() }];

        let json = serde_json::to_string(&to_document(&a)).unwrap();
        let mut b = Settings::default();
        apply(&mut b, &serde_json::from_str(&json).unwrap());

        assert_eq!(b.method, Method::Telex);
        assert_eq!(b.tone_style, ToneStyle::Modern);
        assert_eq!(b.toggle_hotkey, Hotkey::AltShift);
        assert_eq!(b.shortcuts.len(), 1);
        assert_eq!(b.excluded_apps.len(), 1);
    }

    #[test]
    fn merges_shortcuts_by_trigger() {
        let mut s = Settings::default();
        s.shortcuts = vec![Shortcut { trigger: "vn".into(), expansion: "old".into() }];
        let doc: ConfigDocument = serde_json::from_str(
            r#"{"schema":"app.funput.config","version":1,"shortcuts":[{"trigger":"vn","expansion":"Việt Nam"},{"trigger":"kg","expansion":"không"}]}"#,
        )
        .unwrap();
        let summary = apply(&mut s, &doc);
        assert_eq!(summary.shortcuts_added, 1);
        assert_eq!(summary.shortcuts_updated, 1);
        assert_eq!(s.shortcuts.len(), 2);
    }

    #[test]
    fn imports_macos_file_ignoring_its_platform_block() {
        let macos = r#"{
          "schema":"app.funput.config","version":1,
          "preferences":{"inputMethod":"vni","toneStyle":"modern","smartEnglishRestore":false},
          "shortcuts":[{"trigger":"cf","expansion":"cà phê"}],
          "platform":{"macos":{"toggleShortcut":{"keyCode":42,"modifiers":262144,"label":"\\"},"excludedApps":[{"id":"com.apple.Safari","name":"Safari"}]}}
        }"#;
        let mut s = Settings::default();
        let summary = apply(&mut s, &serde_json::from_str(macos).unwrap());
        assert_eq!(s.method, Method::Vni);
        assert_eq!(s.tone_style, ToneStyle::Modern);
        assert!(!s.smart_restore);
        assert_eq!(s.shortcuts.len(), 1);
        assert!(!summary.applied_platform); // no platform.linux
        assert!(s.excluded_apps.is_empty()); // macOS bundleIds not imported
    }

    #[test]
    fn imports_windows_file_ignoring_its_platform_block() {
        let windows = r#"{
          "schema":"app.funput.config","version":1,
          "preferences":{"inputMethod":"telex"},
          "shortcuts":[{"trigger":"vn","expansion":"Việt Nam"}],
          "platform":{"windows":{"toggleHotkey":"ctrl_space","excludedApps":[{"id":"code.exe","name":"VS Code"}]}}
        }"#;
        let mut s = Settings::default();
        let summary = apply(&mut s, &serde_json::from_str(windows).unwrap());
        assert_eq!(s.method, Method::Telex);
        assert!(!summary.applied_platform); // platform.windows ignored on Linux
        assert_eq!(s.toggle_hotkey, Hotkey::CtrlBacktick); // unchanged (default)
        assert!(s.excluded_apps.is_empty()); // Windows exe names not imported
    }
}
