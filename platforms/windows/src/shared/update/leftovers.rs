//! Removing what an update leaves next to the executable.
//!
//! A running executable can be renamed but not deleted, so [`super::install`]
//! moves the old build aside (`Funput.exe.<pid>.old`) and the new build deletes
//! it here when it starts. Only the new build can: the old one is mapped by both
//! the background process and the Settings window until they have both exited.
//!
//! Builds before this one used the `self-replace` crate, whose helper process
//! waited for the Settings window alone and then tried a single delete — which
//! failed whenever the background process was still running from the same file.
//! What it left (`.Funput.<random>.__relocated__.exe` and its helper) sits next to
//! the executable, or in `%TEMP%` when both are on one drive, and is swept here
//! as well.

use std::path::{Path, PathBuf};
use std::time::{Duration, Instant};

use crate::shared::packaged;

const BACKUP_SUFFIX: &str = ".old";
const SELF_REPLACE_SUFFIXES: [&str; 3] =
    [".__relocated__.exe", ".__selfdelete__.exe", ".__temp__.exe"];

/// How often to retry, and for how long. The Settings window of the build being
/// replaced is still exiting when the new one starts, and holds the file till then.
const RETRY_EVERY: Duration = Duration::from_millis(200);
const GIVE_UP_AFTER: Duration = Duration::from_secs(10);

/// The name the running build is moved aside to: `<exe>.<tag>.old`. Not an `.exe`,
/// so it cannot be started by mistake while it waits to be deleted.
pub(super) fn backup_name(exe_name: &str, tag: u32) -> String {
    format!("{exe_name}.{tag}{BACKUP_SUFFIX}")
}

/// Delete every leftover of an earlier update, retrying on a background thread
/// while Windows still holds a file. Call once, from the background process.
pub fn remove_leftovers() {
    if packaged::is_packaged() {
        return; // the Store installs and updates the package itself
    }
    let Ok(exe) = std::env::current_exe() else {
        return;
    };
    let Some(exe_name) = exe.file_name().and_then(|n| n.to_str()).map(str::to_owned) else {
        return;
    };
    let dirs: Vec<PathBuf> = exe
        .parent()
        .map(Path::to_path_buf)
        .into_iter()
        .chain([std::env::temp_dir()])
        .collect();
    std::thread::spawn(move || {
        let deadline = Instant::now() + GIVE_UP_AFTER;
        while !remove_from(&dirs, &exe_name) && Instant::now() < deadline {
            std::thread::sleep(RETRY_EVERY);
        }
    });
}

/// One pass over `dirs`. True when nothing that matched is left.
fn remove_from(dirs: &[PathBuf], exe_name: &str) -> bool {
    let mut clean = true;
    for dir in dirs {
        let Ok(entries) = std::fs::read_dir(dir) else {
            continue;
        };
        for entry in entries.flatten() {
            let name = entry.file_name();
            let Some(name) = name.to_str() else {
                continue;
            };
            if is_leftover(name, exe_name) && std::fs::remove_file(entry.path()).is_err() {
                clean = false;
            }
        }
    }
    clean
}

/// Whether `file_name` is something an update of `exe_name` left behind. Ignores
/// case, like the file system it names.
fn is_leftover(file_name: &str, exe_name: &str) -> bool {
    let file = file_name.to_ascii_lowercase();
    let exe = exe_name.to_ascii_lowercase();
    // This build's own: `funput.exe.<tag>.old`.
    if let Some(tag) = file
        .strip_prefix(&format!("{exe}."))
        .and_then(|rest| rest.strip_suffix(BACKUP_SUFFIX))
    {
        return !tag.is_empty() && tag.bytes().all(|b| b.is_ascii_digit());
    }
    // `self-replace`'s: `.funput.<random><suffix>`.
    let stem = exe.strip_suffix(".exe").unwrap_or(&exe);
    file.strip_prefix(&format!(".{stem}.")).is_some_and(|rest| {
        SELF_REPLACE_SUFFIXES
            .iter()
            .any(|suffix| rest.len() > suffix.len() && rest.ends_with(suffix))
    })
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_backups_an_update_writes_are_leftovers() {
        assert!(is_leftover("Funput.exe.4242.old", "Funput.exe"));
        assert!(is_leftover("FUNPUT.EXE.7.OLD", "Funput.exe"));
        assert!(is_leftover(&backup_name("Funput.exe", 9), "Funput.exe"));
    }

    /// What users of earlier builds already have, next to the exe or in %TEMP%.
    #[test]
    fn self_replace_leftovers_are_leftovers() {
        let random = "czqbgvukvamaootyudamwjfdgcaltevx";
        for suffix in SELF_REPLACE_SUFFIXES {
            assert!(is_leftover(
                &format!(".Funput.{random}{suffix}"),
                "Funput.exe"
            ));
        }
    }

    #[test]
    fn everything_else_is_kept() {
        for name in [
            "Funput.exe",
            "Funput-1.2026.2.exe", // canonical_exe's business, not ours
            "Funput.exe.4242.new", // an update still being written
            "Funput.exe.old",      // no tag: not a name this build writes
            "Funput.exe.notes.old",
            "notes.old",
            ".Other.abc.__relocated__.exe", // another program's
            ".Funput.__relocated__.exe",    // nothing between the stem and suffix
            "settings.json",
        ] {
            assert!(!is_leftover(name, "Funput.exe"), "{name}");
        }
    }

    #[test]
    fn a_pass_deletes_only_leftovers() {
        let dir = std::env::temp_dir().join(format!("funput-leftovers-{}", std::process::id()));
        let _ = std::fs::remove_dir_all(&dir);
        std::fs::create_dir_all(&dir).unwrap();
        for name in [
            "Funput.exe",
            "Funput.exe.1.old",
            ".Funput.x.__relocated__.exe",
            "a.txt",
        ] {
            std::fs::write(dir.join(name), b"").unwrap();
        }

        assert!(remove_from(std::slice::from_ref(&dir), "Funput.exe"));
        let mut left: Vec<_> = std::fs::read_dir(&dir)
            .unwrap()
            .map(|e| e.unwrap().file_name().into_string().unwrap())
            .collect();
        left.sort();
        assert_eq!(left, ["Funput.exe", "a.txt"]);
        let _ = std::fs::remove_dir_all(dir);
    }
}
