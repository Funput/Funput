//! Reading and writing `settings.json`.
//!
//! Both directions are deliberately forgiving: a missing, unreadable, or corrupt
//! file yields [`Settings::default`] rather than an error, and a failed write is
//! dropped. This runs under a keyboard hook where the alternative to a stale
//! setting is a dead IME, so neither path may propagate a failure.

use std::fs;
use std::path::Path;

use super::Settings;

impl Settings {
    /// Load from `path`, falling back to defaults when it is missing or corrupt.
    pub fn load_from(path: &Path) -> Self {
        fs::read_to_string(path)
            .ok()
            .and_then(|json| serde_json::from_str(&json).ok())
            .unwrap_or_default()
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

    #[test]
    fn a_missing_or_corrupt_file_reads_as_defaults() {
        let dir = tmp_dir();
        let missing = dir.join("nope.json");
        assert_eq!(Settings::load_from(&missing), Settings::default());

        let corrupt = dir.join("corrupt.json");
        fs::write(&corrupt, "{ not json").unwrap();
        assert_eq!(Settings::load_from(&corrupt), Settings::default());
        let _ = fs::remove_dir_all(dir);
    }
}
