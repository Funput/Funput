//! What a personal-lexicon file on disk is allowed to look like: its magic bytes,
//! its schema version, and the size past which a file is treated as damaged rather
//! than read into memory.

pub(super) const SNAPSHOT_MAGIC: &[u8; 8] = b"FPSNAP01";
pub(super) const JOURNAL_MAGIC: &[u8; 4] = b"FPJR";
pub(super) const MAX_SNAPSHOT_BYTES: u64 = 2 * 1024 * 1024;
pub(super) const MAX_JOURNAL_BYTES: u64 = 1024 * 1024;

// The two records are separate formats that happened to share a number. They are
// versioned apart so one can move without claiming anything about the other —
// stamping a journal as v2 because the snapshot grew would tell the reader it
// contains context breaks it does not have.

/// v2 carries each word's follower edges; v1 is the word list alone.
pub(super) const SNAPSHOT_WRITE_VERSION: u16 = 2;
pub(super) const SNAPSHOT_MIN_READ_VERSION: u16 = 1;

/// v1 is a flat token list; v2 may carry context breaks between the tokens.
pub(super) const JOURNAL_WRITE_VERSION: u16 = 2;
pub(super) const JOURNAL_MIN_READ_VERSION: u16 = 1;

/// The oldest schema a build can still read. Raising one is how a format is
/// retired: files below it are discarded and rebuilt rather than rejected.
///
/// A build that cannot read what it writes would discard every file on upgrade,
/// so the windows are checked here — a bad one fails the build, not a test.
const _: () = assert!(SNAPSHOT_MIN_READ_VERSION <= SNAPSHOT_WRITE_VERSION);
const _: () = assert!(JOURNAL_MIN_READ_VERSION <= JOURNAL_WRITE_VERSION);

pub(super) enum Version {
    Readable,
    /// Written by a newer build. Its meaning is unknown, so it must be left
    /// untouched — the user may simply be running an older app for a moment.
    TooNew,
    /// Written by a build whose format we have retired. Safe to throw away and
    /// rebuild from what the running engine knows.
    TooOld,
}

pub(super) fn accept_snapshot(version: u16) -> Version {
    verdict(version, SNAPSHOT_MIN_READ_VERSION, SNAPSHOT_WRITE_VERSION)
}

pub(super) fn accept_journal(version: u16) -> Version {
    verdict(version, JOURNAL_MIN_READ_VERSION, JOURNAL_WRITE_VERSION)
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
    fn each_shipped_window_reads_what_it_writes() {
        assert!(matches!(
            accept_snapshot(SNAPSHOT_WRITE_VERSION),
            Version::Readable
        ));
        assert!(matches!(
            accept_snapshot(SNAPSHOT_MIN_READ_VERSION),
            Version::Readable
        ));
        assert!(matches!(
            accept_journal(JOURNAL_WRITE_VERSION),
            Version::Readable
        ));
    }

    #[test]
    fn a_newer_schema_is_never_readable() {
        assert!(matches!(
            accept_snapshot(SNAPSHOT_WRITE_VERSION + 1),
            Version::TooNew
        ));
        assert!(matches!(accept_snapshot(u16::MAX), Version::TooNew));
        assert!(matches!(
            accept_journal(JOURNAL_WRITE_VERSION + 1),
            Version::TooNew
        ));
    }

    #[test]
    fn the_two_records_are_versioned_apart() {
        // They move independently. What matters is that each still reads the
        // version below it, which is every file already in the field.
        assert!(matches!(accept_snapshot(1), Version::Readable));
        assert!(matches!(accept_journal(1), Version::Readable));
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
