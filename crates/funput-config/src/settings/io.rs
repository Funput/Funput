//! Reading and writing `settings.json`.
//!
//! Both directions are deliberately forgiving: a missing, unreadable, or corrupt
//! file yields defaults rather than an error, and a failed write is dropped. This
//! runs under a keyboard hook where the alternative to a stale setting is a dead
//! IME, so neither path may propagate a failure.

use std::fs;
use std::path::Path;

use super::Settings;
use super::model::legacy_tone_style_default;

impl Settings {
    /// Load from `path`. A missing file yields [`Settings::default`]; a corrupt one
    /// yields the same except for the tone placement, which stays where an existing
    /// user had it (see [`legacy_tone_style_default`]).
    ///
    /// This is also where the removed "always English" list is migrated: each id
    /// becomes a remembered English choice, and the field is drained so the key
    /// disappears from disk on the next write. Re-running it before that write is
    /// harmless — the merge keeps whatever is already remembered.
    pub fn load_from(path: &Path) -> Self {
        let mut settings: Self = match fs::read_to_string(path) {
            // A file that will not parse still belongs to someone who has been
            // typing here. The rest of their settings are gone either way, but tone
            // placement is not a thing to guess at, so it keeps the value every file
            // written before the default flipped implied.
            Ok(json) => serde_json::from_str(&json).unwrap_or_else(|_| Self {
                tone_style: legacy_tone_style_default(),
                ..Self::default()
            }),
            Err(_) => Self::default(),
        };
        let legacy: Vec<_> = std::mem::take(&mut settings.excluded_apps);
        settings.remember_as_english(legacy.into_iter().map(|app| app.id));
        settings
    }

    /// Write to `path`, creating its directory. Silent on failure — see above.
    pub fn save_to(&self, path: &Path) {
        if let Some(directory) = path.parent() {
            let _ = fs::create_dir_all(directory);
        }
        if let Ok(json) = serde_json::to_string_pretty(self) {
            let _ = fs::write(path, json);
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::ToneStyle;
    use crate::test_support::unique_dir;

    fn tmp_dir() -> std::path::PathBuf {
        unique_dir("io")
    }

    #[test]
    fn saves_and_loads_back() {
        let dir = tmp_dir();
        let path = dir.join("settings.json");
        let settings = Settings {
            spell_check: true,
            ..Settings::default()
        };
        settings.save_to(&path);

        assert_eq!(Settings::load_from(&path), settings);
        let _ = fs::remove_dir_all(dir);
    }

    #[test]
    fn creates_the_parent_directory() {
        let dir = tmp_dir();
        let path = dir.join("nested").join("deeper").join("settings.json");
        Settings::default().save_to(&path);

        assert!(path.is_file());
        let _ = fs::remove_dir_all(dir);
    }

    /// The list a user built before per-app memory existed has to survive the
    /// upgrade, so every entry comes back as "remembered as English".
    #[test]
    fn legacy_excluded_apps_migrate_to_english_on_load() {
        let dir = tmp_dir();
        let path = dir.join("settings.json");
        let mut json = serde_json::to_value(Settings::default()).unwrap();
        json["excludedApps"] = serde_json::json!([{ "id": "code.exe", "name": "VS Code" }]);
        fs::write(&path, json.to_string()).unwrap();

        let loaded = Settings::load_from(&path);
        assert_eq!(loaded.app_language_memory.get("code.exe"), Some(&false));
        assert!(loaded.excluded_apps.is_empty(), "drained, not kept");
        let _ = fs::remove_dir_all(dir);
    }

    /// An app the user has toggled since keeps that choice — migrating must not
    /// silently force it back to English.
    #[test]
    fn migration_never_overwrites_a_remembered_choice() {
        let mut settings = Settings::default();
        settings
            .app_language_memory
            .insert("code.exe".to_string(), true);
        settings.remember_as_english(["code.exe".to_string(), "chrome.exe".to_string()]);

        assert_eq!(settings.app_language_memory.get("code.exe"), Some(&true));
        assert_eq!(settings.app_language_memory.get("chrome.exe"), Some(&false));
    }

    #[test]
    fn the_legacy_key_is_dropped_on_the_next_save() {
        let dir = tmp_dir();
        let path = dir.join("settings.json");
        let mut json = serde_json::to_value(Settings::default()).unwrap();
        json["excludedApps"] = serde_json::json!([{ "id": "code.exe", "name": "VS Code" }]);
        fs::write(&path, json.to_string()).unwrap();

        Settings::load_from(&path).save_to(&path);

        let written = fs::read_to_string(&path).unwrap();
        assert!(!written.contains("excludedApps"), "{written}");
        assert!(written.contains("appLanguageMemory"), "{written}");
        let _ = fs::remove_dir_all(dir);
    }

    /// A corrupt file resets everything the user can see and re-pick. Tone placement
    /// is the exception: nothing in the UI says it moved, only their typing does.
    #[test]
    fn a_missing_or_corrupt_file_reads_as_defaults() {
        let dir = tmp_dir();
        let missing = dir.join("nope.json");
        assert_eq!(Settings::load_from(&missing), Settings::default());

        let corrupt = dir.join("corrupt.json");
        fs::write(&corrupt, "{ not json").unwrap();
        assert_eq!(
            Settings::load_from(&corrupt),
            Settings {
                tone_style: ToneStyle::Traditional,
                ..Settings::default()
            }
        );
        let _ = fs::remove_dir_all(dir);
    }
}
