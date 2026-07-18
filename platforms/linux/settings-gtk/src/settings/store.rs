use std::fs;
use std::path::PathBuf;

use super::{ExcludedApp, Settings};

fn settings_path() -> Option<PathBuf> {
    dirs::config_dir().map(|dir| dir.join("Funput").join("settings.json"))
}

pub fn recent_apps() -> Vec<ExcludedApp> {
    dirs::config_dir()
        .map(|dir| dir.join("Funput").join("recent-apps.json"))
        .and_then(|path| fs::read_to_string(path).ok())
        .and_then(|json| serde_json::from_str(&json).ok())
        .unwrap_or_default()
}

impl Settings {
    pub fn load() -> Self {
        settings_path()
            .and_then(|path| fs::read_to_string(path).ok())
            .and_then(|json| serde_json::from_str(&json).ok())
            .unwrap_or_default()
    }

    pub fn save(&self) {
        let Some(path) = settings_path() else {
            return;
        };
        if let Some(dir) = path.parent() {
            let _ = fs::create_dir_all(dir);
        }
        if let Ok(json) = serde_json::to_string_pretty(self) {
            let _ = fs::write(path, json);
        }
    }

    pub fn update(f: impl FnOnce(&mut Settings)) -> Settings {
        let mut settings = Settings::load();
        f(&mut settings);
        settings.save();
        settings
    }
}
