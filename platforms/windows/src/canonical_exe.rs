//! Normalize versioned release assets (`Funput-1.x.y.exe`) to a stable `Funput.exe`.
//!
//! GitHub keeps versioned names for humans; the running tray process should always
//! live at the sibling canonical path so autostart and updates stay coherent.

use std::path::{Path, PathBuf};
use std::process::Command;

const CANONICAL: &str = "Funput.exe";

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
/// spawn it, and return `true` so the caller can exit.
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
    Command::new(&canonical).spawn().is_ok()
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
