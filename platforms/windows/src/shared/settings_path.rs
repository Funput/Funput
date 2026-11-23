//! Where this Windows install keeps `settings.json`, and when it last changed.
//!
//! The decision itself lives in [`funput_config::settings::path::resolve`]; this
//! supplies the three OS facts it needs (the env override, the executable's
//! directory, whether that directory is writable) and the AppData fallback.
//! Splitting it that way is what lets the policy be unit-tested off Windows.

use std::path::{Path, PathBuf};
use std::sync::OnceLock;
use std::time::SystemTime;

use funput_config::settings::path::{FILE, dir_is_writable, resolve};

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
///
/// Resolved once per process. The answer cannot change while it runs, and finding
/// it writes a probe file into the exe directory, which the watchers polling
/// [`stamp`] must not do several times a second.
pub fn settings_path() -> Option<&'static Path> {
    static PATH: OnceLock<Option<PathBuf>> = OnceLock::new();
    PATH.get_or_init(resolve_path).as_deref()
}

fn resolve_path() -> Option<PathBuf> {
    let exe_dir = exe_parent();
    let writable = exe_dir.as_deref().is_some_and(dir_is_writable);
    resolve(
        std::env::var_os("FUNPUT_CONFIG").map(PathBuf::from),
        exe_dir.as_deref(),
        writable,
        appdata_settings().as_deref(),
    )
}

/// What a watcher compares to tell that another process saved the file.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct Stamp {
    modified: SystemTime,
    len: u64,
}

/// The settings file's [`Stamp`]: one metadata call, nothing read. `None` while
/// there is no file.
pub fn stamp() -> Option<Stamp> {
    let meta = std::fs::metadata(settings_path()?).ok()?;
    Some(Stamp {
        modified: meta.modified().ok()?,
        len: meta.len(),
    })
}
