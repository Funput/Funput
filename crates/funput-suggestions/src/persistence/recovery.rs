//! Reading a store that may be damaged.
//!
//! Every branch here answers one question: is this file something we can still
//! read, something we should throw away and rebuild, or something we must not
//! touch? Keeping that judgement in one module is what stops each call site from
//! inventing its own answer.
//!
//! The keyboard opens a store to write it and repairs what it finds; the app only
//! reads one the keyboard may be writing at that moment, so it repairs nothing.

use std::fs::{self, File};
use std::io::{self, Read};
use std::path::Path;

use super::codec::journal;
use super::codec::snapshot::{self, Snapshot};
use super::fs::{sync_dir, truncate};
use super::schema::{MAX_JOURNAL_BYTES, MAX_SNAPSHOT_BYTES};
use super::{JournalEntry, Loaded, Store};
use crate::binary::Cursor;
use crate::ngram::{PROBE_WINDOW, TrigramTable};

/// Whether a reader may change the files it reads.
#[derive(Clone, Copy, PartialEq, Eq)]
pub(crate) enum Access {
    /// The writer: damage is repaired as it is found.
    Repair,
    /// Someone else's store: damage is skipped, never touched.
    ReadOnly,
}

impl Store {
    pub(crate) fn open(root: &Path) -> io::Result<(Self, Loaded)> {
        std::fs::create_dir_all(root)?;
        let store = Self {
            root: root.to_owned(),
        };
        let loaded = store.load(Access::Repair)?;
        Ok((store, loaded))
    }

    /// Reads a store without writing anything, not even a missing directory.
    pub(crate) fn read(root: &Path) -> io::Result<Loaded> {
        let store = Self {
            root: root.to_owned(),
        };
        store.load(Access::ReadOnly)
    }

    fn load(&self, access: Access) -> io::Result<Loaded> {
        let Some(snapshot) = self.load_snapshot()? else {
            // The snapshot was damaged, not absent. Repair both files now: leaving
            // the snapshot means every later open discards the journal again, and
            // leaving the journal means its unreadable head swallows every valid
            // frame appended after it. Best effort — an empty engine works either
            // way, and the next flush will try again.
            if access == Access::Repair {
                let _ = self.compact(&[], &[], &TrigramTable::new(PROBE_WINDOW), 0);
            }
            return Ok(Loaded::default());
        };
        let (journal, journal_bytes) = self.load_journal(snapshot.sequence, access)?;
        Ok(Loaded {
            words: snapshot.words,
            blocked: snapshot.blocked,
            trigrams: snapshot.trigrams,
            sequence: snapshot.sequence,
            journal,
            journal_bytes,
        })
    }

    fn load_snapshot(&self) -> io::Result<Option<Snapshot>> {
        let mut bytes = Vec::new();
        match File::open(self.snapshot_path()) {
            Ok(mut file) => {
                if file.metadata()?.len() > MAX_SNAPSHOT_BYTES {
                    return Ok(None);
                }
                file.read_to_end(&mut bytes)?;
            }
            Err(error) if error.kind() == io::ErrorKind::NotFound => {
                return Ok(Some(Snapshot::default()));
            }
            Err(error) => return Err(error),
        }
        snapshot::decode(&bytes)
    }

    /// Frames the snapshot already contains are skipped rather than replayed.
    ///
    /// `compact` writes the snapshot and truncates the journal as two separate
    /// writes; a crash between them leaves a journal that has already been folded
    /// in. Comparing the sequence each frame ends at against the snapshot's makes
    /// replaying such a journal a no-op instead of counting every token twice.
    fn load_journal(
        &self,
        snapshot_sequence: u64,
        access: Access,
    ) -> io::Result<(Vec<JournalEntry>, u64)> {
        let path = self.journal_path();
        let bytes = match fs::metadata(&path) {
            // Far past the 64 KiB that triggers a compact, so this is not
            // something we wrote. Dropping it costs the tokens since the last
            // snapshot; returning an error costs the shells their persistence for
            // the rest of the process.
            Ok(metadata) if metadata.len() > MAX_JOURNAL_BYTES => {
                if access == Access::Repair {
                    self.discard_journal()?;
                }
                return Ok((Vec::new(), 0));
            }
            Ok(_) => fs::read(path)?,
            Err(error) if error.kind() == io::ErrorKind::NotFound => return Ok((Vec::new(), 0)),
            Err(error) => return Err(error),
        };
        let journal_bytes = bytes.len() as u64;
        let mut cursor = Cursor::new(&bytes);
        let mut entries = Vec::new();
        while !cursor.is_empty() {
            match journal::decode_frame(&mut cursor) {
                // A frame from before v3 records no sequence, so nothing in it
                // says whether it has been folded in; replay it, as every build
                // before this one did.
                Ok(frame) if frame.sequence.is_none_or(|at| at > snapshot_sequence) => {
                    entries.extend(frame.entries);
                }
                Ok(_) => {}
                Err(error) if error.kind() == io::ErrorKind::Unsupported => return Err(error),
                Err(_) => break,
            }
        }
        Ok((entries, journal_bytes))
    }

    /// Empty a journal we have decided not to read.
    ///
    /// Leaving it in place is not neutral: `load_journal` stops at the first frame
    /// that fails to decode, so garbage at the head silently swallows every valid
    /// frame appended after it, for as long as the file survives.
    fn discard_journal(&self) -> io::Result<()> {
        truncate(&self.journal_path())?;
        sync_dir(&self.root)
    }
}
