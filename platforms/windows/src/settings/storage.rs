use std::fs;
use std::path::PathBuf;

use super::Settings;

fn settings_path() -> Option<PathBuf> {
    dirs::config_dir().map(|directory| directory.join("Funput").join("settings.json"))
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
        if let Some(directory) = path.parent() {
            let _ = fs::create_dir_all(directory);
        }
        if let Ok(json) = serde_json::to_string_pretty(self) {
            let _ = fs::write(path, json);
        }
    }
}
