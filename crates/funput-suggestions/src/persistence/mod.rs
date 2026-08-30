//! The crash-safe on-disk store: a checksummed snapshot of the whole word list,
//! plus an append-only journal of everything learned since it was written.
//!
//! - `schema` — the on-disk constants both records agree on.
//! - `codec` — pure bytes to data translation, no filesystem.
//! - `recovery` — reading a store that may be damaged.
//! - `fs` — the filesystem calls that have to be durable, not merely successful.
//! - here — paths, and the two write paths (`append_journal`, `compact`).

mod codec;
mod fs;
mod recovery;
mod schema;

use std::fs::{File, OpenOptions};
use std::io::{self, Write};
use std::path::{Path, PathBuf};

use crate::types::WordRecord;
use codec::journal::Entry;
use codec::{journal, snapshot};

pub(crate) use codec::journal::Entry as JournalEntry;

pub(crate) struct Store {
    pub(super) root: PathBuf,
}

pub(crate) struct Loaded {
    pub(crate) words: Vec<WordRecord>,
    pub(crate) sequence: u64,
    pub(crate) journal: Vec<Entry>,
    pub(crate) journal_bytes: u64,
}

impl Store {
    pub(crate) fn open(root: &Path) -> io::Result<(Self, Loaded)> {
        std::fs::create_dir_all(root)?;
        let store = Self {
            root: root.to_owned(),
        };
        let snapshot = store.load_snapshot()?;
        let (words, sequence, snapshot_valid) = match snapshot {
            Some((words, sequence)) => (words, sequence, true),
            None => (Vec::new(), 0, false),
        };
        let (journal, journal_bytes) = if snapshot_valid {
            store.load_journal()?
        } else {
            // The snapshot was damaged, not absent. Repair both files now: leaving
            // the snapshot means every later open discards the journal again, and
            // leaving the journal means its unreadable head swallows every valid
            // frame appended after it. Best effort — an empty engine works either
            // way, and the next flush will try again.
            let _ = store.compact(&[], 0);
            (Vec::new(), 0)
        };
        Ok((
            store,
            Loaded {
                words,
                sequence,
                journal,
                journal_bytes,
            },
        ))
    }

    pub(crate) fn append_journal(&self, entries: &[Entry]) -> io::Result<u64> {
        if entries.is_empty() {
            return Ok(0);
        }
        let frame = journal::encode_frame(entries);
        let path = self.journal_path();
        // A brand new journal is a directory change, so the entry needs syncing
        // too — once, on the append that creates it, not on every later one.
        let created = !path.exists();
        let mut file = OpenOptions::new().create(true).append(true).open(&path)?;
        file.write_all(&frame)?;
        file.sync_data()?;
        if created {
            fs::sync_dir(&self.root)?;
        }
        Ok(frame.len() as u64)
    }

    pub(crate) fn compact(&self, words: &[WordRecord], sequence: u64) -> io::Result<u64> {
        let bytes = snapshot::encode(words, sequence);
        let temporary = self.root.join("personal-lexicon.snapshot.tmp");
        let mut file = File::create(&temporary)?;
        file.write_all(&bytes)?;
        file.sync_all()?;
        std::fs::rename(&temporary, self.snapshot_path())?;
        let journal = File::create(self.journal_path())?;
        journal.sync_all()?;
        // Both the rename and the fresh journal are directory changes: without
        // this the snapshot we just fsynced can still be the one lost.
        fs::sync_dir(&self.root)?;
        Ok(bytes.len() as u64)
    }

    pub(super) fn snapshot_path(&self) -> PathBuf {
        self.root.join("personal-lexicon.snapshot")
    }

    pub(super) fn journal_path(&self) -> PathBuf {
        self.root.join("personal-lexicon.journal")
    }
}

#[cfg(test)]
pub(crate) use codec::binary::checksum;
