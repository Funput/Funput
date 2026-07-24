//! Persistent configuration for `funput-term`.
//!
//! Reads the project's canonical settings file —
//! `dirs::config_dir()/Funput/settings.json` — so on Linux/Windows the terminal
//! inherits the system IME's preferences, and on macOS it uses its own file at the
//! same well-known path.
//!
//! Resolution precedence is **CLI flag > env var > settings.json > built-in
//! default**. The on-disk `schema` and the env overlay (`parse`) live in
//! submodules; the CLI layer is applied by the caller (`funput-cli`).
//!
//! The parse, env-overlay, and engine-apply steps are pure seams (no real I/O), so
//! they are unit-tested directly.

use std::path::PathBuf;

use funput_core::{InputMethod, ToneStyle};
use funput_engine::Engine;

use crate::terminal::DEFAULT_VI_CURSOR_COLOR;

mod parse;
mod schema;

use schema::FileSettings;

/// `Ctrl-\` (0x1c) — the default Vietnamese on/off toggle byte.
pub const DEFAULT_TOGGLE: u8 = 0x1c;

/// `Ctrl-^` (0x1e) — the default key to cycle Telex↔VNI. Almost never used by
/// shells or readline, so it is safe to claim by default; set `FUNPUT_CYCLE_METHOD`
/// to another key, or to `off`/`none` to disable.
pub const DEFAULT_CYCLE_METHOD: Option<u8> = Some(0x1e);

/// Resolved, engine-ready configuration.
#[derive(Debug, Clone, PartialEq, Eq)]
pub struct TermConfig {
    pub method: InputMethod,
    pub tone_style: ToneStyle,
    /// Whether composition starts on (VI) or off (EN).
    pub enabled: bool,
    pub smart_restore: bool,
    pub eager_restore: bool,
    pub spell_check: bool,
    pub auto_capitalize: bool,
    pub shortcuts: Vec<(String, String)>,
    /// The byte that toggles VI/EN.
    pub toggle: u8,
    /// The byte that cycles Telex↔VNI at runtime, or `None` to disable.
    pub cycle_method: Option<u8>,
    /// Cursor color shown while composing (VI).
    pub vi_cursor_color: String,
}

impl Default for TermConfig {
    fn default() -> Self {
        // Parsing an empty object routes through the serde defaults so the
        // defaults are defined in exactly one place.
        from_json("{}")
    }
}

impl From<FileSettings> for TermConfig {
    fn from(f: FileSettings) -> Self {
        TermConfig {
            method: f.method.into(),
            tone_style: f.tone_style.into(),
            enabled: f.enabled,
            smart_restore: f.smart_restore,
            eager_restore: f.eager_restore,
            spell_check: f.spell_check,
            auto_capitalize: f.auto_capitalize,
            shortcuts: f
                .shortcuts
                .into_iter()
                .map(|s| (s.trigger, s.expansion))
                .collect(),
            toggle: DEFAULT_TOGGLE,
            cycle_method: DEFAULT_CYCLE_METHOD,
            vi_cursor_color: DEFAULT_VI_CURSOR_COLOR.to_string(),
        }
    }
}

/// Parse `settings.json` contents into a resolved config. Malformed or empty input
/// falls back to the canonical defaults.
pub fn from_json(s: &str) -> TermConfig {
    serde_json::from_str::<FileSettings>(s)
        .or_else(|_| serde_json::from_str::<FileSettings>("{}"))
        .expect("an empty JSON object is always valid FileSettings")
        .into()
}

impl TermConfig {
    /// Push the engine-affecting options into a fresh engine. Replaces the whole
    /// shortcut table so repeated calls stay consistent.
    pub fn apply_to(&self, engine: &mut Engine) {
        engine.set_method(self.method);
        engine.set_tone_style(self.tone_style);
        engine.set_smart_restore(self.smart_restore);
        engine.set_eager_restore(self.eager_restore);
        engine.set_spell_check(self.spell_check);
        engine.set_auto_capitalize(self.auto_capitalize);
        engine.clear_shortcuts();
        for (trigger, expansion) in &self.shortcuts {
            engine.add_shortcut(trigger.clone(), expansion.clone());
        }
    }
}

/// Path to the settings file: `$FUNPUT_CONFIG` if set, else the canonical
/// `dirs::config_dir()/Funput/settings.json`.
pub fn settings_path() -> Option<PathBuf> {
    if let Some(p) = std::env::var_os("FUNPUT_CONFIG") {
        return Some(PathBuf::from(p));
    }
    dirs::config_dir().map(|d| d.join("Funput").join("settings.json"))
}

/// Load the effective config: settings file (or defaults if missing/unreadable)
/// with environment overrides applied.
pub fn load() -> TermConfig {
    let from_file = settings_path()
        .and_then(|p| std::fs::read_to_string(p).ok())
        .map(|s| from_json(&s))
        .unwrap_or_default();
    from_file.apply_env(|k| std::env::var(k).ok())
}

#[cfg(test)]
mod tests;
