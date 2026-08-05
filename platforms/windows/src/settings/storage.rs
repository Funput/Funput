//! Persist [`Settings`] next to the executable when the install folder is
//! writable (portable layout). Falls back to AppData; `$FUNPUT_CONFIG` wins.

use std::fs;
use std::path::{Path, PathBuf};

use super::Settings;

const FILE: &str = "settings.json";
const PROBE: &str = ".funput-write-probe";

fn appdata_settings() -> Option<PathBuf> {
    dirs::config_dir().map(|directory| directory.join("Funput").join(FILE))
}

fn exe_parent() -> Option<PathBuf> {
    std::env::current_exe()
        .ok()
        .and_then(|exe| exe.parent().map(Path::to_path_buf))
}

fn dir_is_writable(dir: &Path) -> bool {
    let probe = dir.join(PROBE);
    if fs::write(&probe, b"").is_err() {
        return false;
    }
    let _ = fs::remove_file(&probe);
    true
}

/// Resolve where settings live. Pure enough to unit-test with temp dirs:
/// 1. `env` (`$FUNPUT_CONFIG`) if set
/// 2. `<exe_dir>/settings.json` when `exe_writable`
/// 3. else `appdata` (`%APPDATA%\Funput\settings.json`)
///
/// When choosing the beside-exe path and the file is missing, copy from AppData
/// once (AppData is left in place).
pub(crate) fn resolve(
    env: Option<PathBuf>,
    exe_dir: Option<&Path>,
    exe_writable: bool,
    appdata: Option<&Path>,
) -> Option<PathBuf> {
    if let Some(path) = env {
        return Some(path);
    }
    if let Some(dir) = exe_dir.filter(|_| exe_writable) {
        let path = dir.join(FILE);
        if !path.exists() {
            if let Some(src) = appdata.filter(|p| p.is_file()) {
                let _ = fs::copy(src, &path);
            }
        }
        return Some(path);
    }
    appdata.map(Path::to_path_buf)
}

fn settings_path() -> Option<PathBuf> {
    let exe_dir = exe_parent();
    let writable = exe_dir.as_deref().is_some_and(dir_is_writable);
    resolve(
        std::env::var_os("FUNPUT_CONFIG").map(PathBuf::from),
        exe_dir.as_deref(),
        writable,
        appdata_settings().as_deref(),
    )
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

#[cfg(test)]
mod tests;
