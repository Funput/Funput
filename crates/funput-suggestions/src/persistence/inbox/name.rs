//! What an edit file is called, which is the order it is applied in.
//!
//! `<stamp>-<pid>-<counter>.edit`, zero-padded so names sort as numbers. The stamp
//! is the wall clock in nanoseconds, but never earlier than one past the newest
//! file still waiting: a clock stepped backwards between two posts must not let the
//! later one sort first, or a Block posted before an Unblock would be applied after
//! it. Files already drained are gone and no longer matter to the order.

use std::ffi::OsStr;
use std::fs;
use std::io;
use std::path::Path;
use std::sync::atomic::{AtomicU32, Ordering};
use std::time::{SystemTime, UNIX_EPOCH};

pub(super) const EXTENSION: &str = "edit";
/// What a file is called while it is still being written.
pub(super) const TEMPORARY: &str = "tmp";

/// The name for the next file posted into `directory`.
pub(super) fn next(directory: &Path) -> io::Result<String> {
    static COUNTER: AtomicU32 = AtomicU32::new(0);
    let now = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map_or(0, |elapsed| elapsed.as_nanos());
    let newest = newest_stamp(directory)?;
    let stamp = newest.map_or(now, |newest| now.max(newest.saturating_add(1)));
    let count = COUNTER.fetch_add(1, Ordering::Relaxed);
    Ok(format!(
        "{stamp:039}-{:010}-{count:010}.{EXTENSION}",
        std::process::id()
    ))
}

pub(super) fn is_edit_file(path: &Path) -> bool {
    path.extension()
        .is_some_and(|extension| extension == EXTENSION)
}

/// Counts files still being written too, so one renamed into place after this post
/// was named still sorts before it.
fn newest_stamp(directory: &Path) -> io::Result<Option<u128>> {
    let entries = match fs::read_dir(directory) {
        Ok(entries) => entries,
        Err(error) if error.kind() == io::ErrorKind::NotFound => return Ok(None),
        Err(error) => return Err(error),
    };
    Ok(entries
        .filter_map(Result::ok)
        .filter_map(|entry| stamp(&entry.file_name()))
        .max())
}

fn stamp(name: &OsStr) -> Option<u128> {
    name.to_str()?.split('-').next()?.parse().ok()
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_name_never_sorts_before_a_file_still_waiting() {
        let directory = tempfile::tempdir().unwrap();
        let ahead = format!("{:039}-0-0.{EXTENSION}", u128::MAX / 2);
        fs::write(directory.path().join(&ahead), b"").unwrap();
        let next = next(directory.path()).unwrap();
        assert!(next > ahead, "{next} must sort after {ahead}");
        assert!(is_edit_file(Path::new(&next)));
        assert!(!is_edit_file(&Path::new(&next).with_extension(TEMPORARY)));
    }
}
