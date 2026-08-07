//! Normalize versioned release assets (`Funput-1.x.y.exe`) to a stable `Funput.exe`.
//!
//! GitHub keeps versioned names for humans; the running tray process should always
//! live at the sibling canonical path so autostart and updates stay coherent.

use std::path::{Path, PathBuf};
use std::process::Command;
use std::time::Duration;

const CANONICAL: &str = "Funput.exe";
const REMOVE_ENV: &str = "FUNPUT_REMOVE_EXE";

/// True for release-style names like `Funput-1.2026.2.exe` (case-insensitive).
pub fn is_versioned_name(file_name: &str) -> bool {
    let lower = file_name.to_ascii_lowercase();
    if !lower.ends_with(".exe") {
        return false;
    }
    let stem = &lower[..lower.len() - 4];
    stem.starts_with("funput-") && stem != "funput"
}

/// If this process was started from a versioned asset, copy to `Funput.exe`,
/// spawn it (asking it to delete this file), and return `true` so the caller exits.
pub fn normalize_and_relaunch() -> bool {
    let Ok(current) = std::env::current_exe() else {
        return false;
    };
    let Some(name) = current.file_name().and_then(|n| n.to_str()) else {
        return false;
    };
    if !is_versioned_name(name) {
        return false;
    }
    let Some(canonical) = canonical_beside(&current) else {
        return false;
    };
    if std::fs::copy(&current, &canonical).is_err() {
        return false;
    }
    Command::new(&canonical)
        .env(REMOVE_ENV, &current)
        .spawn()
        .is_ok()
}

/// Delete leftover `Funput-*.exe` next to the canonical binary (retry while Windows
/// still holds a lock on the process that just exited).
pub fn cleanup_versioned_siblings() {
    let Ok(current) = std::env::current_exe() else {
        return;
    };
    let Some(name) = current.file_name().and_then(|n| n.to_str()) else {
        return;
    };
    if is_versioned_name(name) {
        return;
    }
    let Some(dir) = current.parent() else {
        return;
    };

    for _ in 0..20 {
        let mut pending = marked_removal();
        if let Ok(entries) = std::fs::read_dir(dir) {
            for entry in entries.flatten() {
                let path = entry.path();
                let Some(fname) = path.file_name().and_then(|n| n.to_str()) else {
                    continue;
                };
                if is_versioned_name(fname) {
                    pending.push(path);
                }
            }
        }
        pending.sort();
        pending.dedup();
        let mut failed = false;
        for path in &pending {
            if std::fs::remove_file(path).is_err() {
                failed = true;
            }
        }
        if !failed {
            return;
        }
        std::thread::sleep(Duration::from_millis(50));
    }
}

fn marked_removal() -> Vec<PathBuf> {
    std::env::var_os(REMOVE_ENV)
        .map(PathBuf::from)
        .filter(|p| {
            p.file_name()
                .and_then(|n| n.to_str())
                .is_some_and(is_versioned_name)
        })
        .into_iter()
        .collect()
}

fn canonical_beside(current: &Path) -> Option<PathBuf> {
    Some(current.parent()?.join(CANONICAL))
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn versioned_release_names() {
        assert!(is_versioned_name("Funput-1.2026.2.exe"));
        assert!(is_versioned_name("funput-1.2026.2.EXE"));
        assert!(is_versioned_name("Funput-dev.exe"));
    }

    #[test]
    fn canonical_and_unrelated_names() {
        assert!(!is_versioned_name("Funput.exe"));
        assert!(!is_versioned_name("funput.exe"));
        assert!(!is_versioned_name("FUNPUT.EXE"));
        assert!(!is_versioned_name("funput"));
        assert!(!is_versioned_name("Other-1.0.exe"));
        assert!(!is_versioned_name("Funput.exe.bak"));
    }
}
