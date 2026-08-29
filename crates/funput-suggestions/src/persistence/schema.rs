//! What a personal-lexicon file on disk is allowed to look like: its magic bytes,
//! its schema version, and the size past which a file is treated as damaged rather
//! than read into memory.

pub(super) const SNAPSHOT_MAGIC: &[u8; 8] = b"FPSNAP01";
pub(super) const JOURNAL_MAGIC: &[u8; 4] = b"FPJR";
pub(super) const MAX_SNAPSHOT_BYTES: u64 = 2 * 1024 * 1024;
pub(super) const MAX_JOURNAL_BYTES: u64 = 1024 * 1024;

/// The schema every file this build writes is stamped with.
pub(super) const WRITE_VERSION: u16 = 1;

/// The oldest schema this build can still read. Raising it is how a format is
/// retired: files below it are discarded and rebuilt rather than rejected.
pub(super) const MIN_READ_VERSION: u16 = 1;

pub(super) enum Version {
    Readable,
    /// Written by a newer build. Its meaning is unknown, so it must be left
    /// untouched — the user may simply be running an older app for a moment.
    TooNew,
    /// Written by a build whose format we have retired. Safe to throw away and
    /// rebuild from what the running engine knows.
    TooOld,
}

pub(super) fn accept(version: u16) -> Version {
    verdict(version, MIN_READ_VERSION, WRITE_VERSION)
}

/// The window is a parameter so the verdict can be exercised across ranges this
/// build does not happen to have yet. A gate whose arms are unreachable today is
/// a gate nobody has tested by the time a migration needs it.
fn verdict(version: u16, min_read: u16, write: u16) -> Version {
    if version > write {
        Version::TooNew
    } else if version < min_read {
        Version::TooOld
    } else {
        Version::Readable
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn the_shipped_window_reads_what_it_writes() {
        assert!(matches!(accept(WRITE_VERSION), Version::Readable));
        assert!(matches!(accept(MIN_READ_VERSION), Version::Readable));
        assert!(MIN_READ_VERSION <= WRITE_VERSION);
    }

    #[test]
    fn a_newer_schema_is_never_readable() {
        assert!(matches!(accept(WRITE_VERSION + 1), Version::TooNew));
        assert!(matches!(accept(u16::MAX), Version::TooNew));
    }

    #[test]
    fn a_window_wider_than_one_reads_every_version_inside_it() {
        for version in 2..=4 {
            assert!(matches!(verdict(version, 2, 4), Version::Readable));
        }
        assert!(matches!(verdict(1, 2, 4), Version::TooOld));
        assert!(matches!(verdict(5, 2, 4), Version::TooNew));
    }
}
