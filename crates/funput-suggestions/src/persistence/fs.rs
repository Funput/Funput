//! Filesystem calls that have to be durable, not merely successful.
//!
//! `write` + `sync_data` only promises the bytes reached the disk — not that the
//! *name* pointing at them did. A rename or a create is a directory change, and
//! it needs the directory itself synced before a power loss is survivable.

use std::fs::File;
use std::io;
use std::path::Path;

/// Flush the directory entry itself, so a rename or create that just happened
/// survives a crash.
///
/// Opening a directory as a file is a POSIX facility; on other platforms this is
/// a no-op, which is the same guarantee those platforms gave us before.
#[cfg(unix)]
pub(super) fn sync_dir(root: &Path) -> io::Result<()> {
    File::open(root)?.sync_all()
}

#[cfg(not(unix))]
pub(super) fn sync_dir(_root: &Path) -> io::Result<()> {
    Ok(())
}

/// Empty a file that exists, without caring whether it did.
pub(super) fn truncate(path: &Path) -> io::Result<()> {
    match File::create(path) {
        Ok(file) => file.sync_all(),
        Err(error) if error.kind() == io::ErrorKind::NotFound => Ok(()),
        Err(error) => Err(error),
    }
}
