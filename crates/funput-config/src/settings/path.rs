//! Where `settings.json` lives.
//!
//! A portable install keeps its config beside the executable so the whole app
//! travels on a USB stick; a per-user install cannot write there and falls back to
//! the OS config directory. [`resolve`] is the decision between them, and it takes
//! every input as an argument — no `current_exe`, no `config_dir` — so the shell
//! supplies the OS facts and this stays testable anywhere.

use std::fs;
use std::path::{Path, PathBuf};

/// The config file name, in whichever directory [`resolve`] settles on.
pub const FILE: &str = "settings.json";

/// Name of the throwaway file [`dir_is_writable`] creates and deletes.
const PROBE: &str = ".funput-write-probe";

/// Whether `dir` accepts writes, tested by actually writing — permissions alone
/// do not answer this on Windows (virtualized Program Files, read-only media).
pub fn dir_is_writable(dir: &Path) -> bool {
    let probe = dir.join(PROBE);
    if fs::write(&probe, b"").is_err() {
        return false;
    }
    let _ = fs::remove_file(&probe);
    true
}

/// Resolve where settings live:
/// 1. `env` (`$FUNPUT_CONFIG`) if set
/// 2. `<exe_dir>/settings.json` when `exe_writable`
/// 3. else `appdata` (`%APPDATA%\Funput\settings.json`)
///
/// When choosing the beside-exe path and the file is missing, copy from `appdata`
/// once so an existing install keeps its settings (`appdata` is left in place).
pub fn resolve(
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
        if let Some(src) = appdata.filter(|p| p.is_file() && !path.exists()) {
            let _ = fs::copy(src, &path);
        }
        return Some(path);
    }
    appdata.map(Path::to_path_buf)
}

#[cfg(test)]
mod tests;
