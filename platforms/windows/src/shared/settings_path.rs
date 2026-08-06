//! Where this Windows install keeps `settings.json`.
//!
//! The decision itself lives in [`funput_config::settings::path::resolve`]; this
//! supplies the three OS facts it needs (the env override, the executable's
//! directory, whether that directory is writable) and the AppData fallback.
//! Splitting it that way is what lets the policy be unit-tested off Windows.

use std::path::{Path, PathBuf};

use funput_config::settings::path::{dir_is_writable, resolve, FILE};

fn appdata_settings() -> Option<PathBuf> {
    dirs::config_dir().map(|directory| directory.join("Funput").join(FILE))
}

fn exe_parent() -> Option<PathBuf> {
    std::env::current_exe()
        .ok()
        .and_then(|exe| exe.parent().map(Path::to_path_buf))
}

/// The settings file for this install, or `None` when neither location is usable
/// (no exe directory and no AppData — settings then stay in memory only).
pub fn settings_path() -> Option<PathBuf> {
    let exe_dir = exe_parent();
    let writable = exe_dir.as_deref().is_some_and(dir_is_writable);
    resolve(
        std::env::var_os("FUNPUT_CONFIG").map(PathBuf::from),
        exe_dir.as_deref(),
        writable,
        appdata_settings().as_deref(),
    )
}
