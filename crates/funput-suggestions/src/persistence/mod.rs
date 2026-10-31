//! The crash-safe on-disk store: a checksummed snapshot of the whole word list,
//! plus an append-only journal of everything learned since it was written.
//!
//! - `schema` — the on-disk constants both records agree on.
//! - `codec` — pure bytes to data translation, no filesystem.
//! - `recovery` — reading a store that may be damaged.
//! - `fs` — the filesystem calls that have to be durable, not merely successful.
//! - `inbox` — the edit files the app posts and the keyboard drains.
//! - here — paths, and the two write paths (`append_journal`, `compact`).

mod codec;
mod fs;
pub(crate) mod inbox;
mod recovery;
mod schema;

use std::fs::{File, OpenOptions};
use std::io::{self, Write};
use std::path::{Path, PathBuf};

use crate::ngram::{Entry, TrigramTable};
use crate::types::WordRecord;
use codec::{journal, snapshot};

pub(crate) use codec::record::Record as JournalEntry;
pub(crate) use recovery::Access;
#[cfg(test)]
pub(crate) use schema::MAX_INBOX_FILES;

pub(crate) struct Store {
    pub(super) root: PathBuf,
}

/// Everything a store holds once read: the snapshot, and the journal entries it
/// does not yet contain.
#[derive(Default)]
pub(crate) struct Loaded {
    pub(crate) words: Vec<WordRecord>,
    pub(crate) blocked: Vec<String>,
    pub(crate) trigrams: Vec<Entry<2>>,
    pub(crate) sequence: u64,
    pub(crate) journal: Vec<JournalEntry>,
    pub(crate) journal_bytes: u64,
}

impl Store {
    pub(crate) fn root(&self) -> &Path {
        &self.root
    }

    pub(crate) fn append_journal(
        &self,
        entries: &[JournalEntry],
        sequence: u64,
    ) -> io::Result<u64> {
        if entries.is_empty() {
            return Ok(0);
        }
        let frame = journal::encode_frame(entries, sequence);
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

    pub(crate) fn compact(
        &self,
        words: &[WordRecord],
        blocked: &[String],
        trigrams: &TrigramTable,
        sequence: u64,
    ) -> io::Result<u64> {
        let bytes = snapshot::encode(words, blocked, trigrams, sequence);
        let temporary = self.root.join("personal-lexicon.snapshot.tmp");
        let mut file = File::create(&temporary)?;
        file.write_all(&bytes)?;
        file.sync_all()?;
        std::fs::rename(&temporary, self.snapshot_path())?;

        // The barrier, and the reason it sits here rather than at the end.
        //
        // Emptying the journal is only safe once the snapshot that replaces it is
        // durable. Nothing orders a rename against the truncation that follows it,
        // so with one sync at the end a crash could leave the journal empty while
        // the *old* snapshot is still the one on disk — and everything learned
        // since the last compact would be gone. Syncing here makes that ordering
        // impossible rather than unlikely.
        //
        // The truncation needs no directory sync of its own. `sync_all` below
        // makes the emptied file durable, and if the journal was being created
        // rather than truncated then losing its directory entry leaves no journal
        // at all, which reads exactly like the empty one it would have been.
        fs::sync_dir(&self.root)?;

        let journal = File::create(self.journal_path())?;
        journal.sync_all()?;
        Ok(bytes.len() as u64)
    }

    pub(super) fn snapshot_path(&self) -> PathBuf {
        self.root.join("personal-lexicon.snapshot")
    }

    pub(super) fn journal_path(&self) -> PathBuf {
        self.root.join("personal-lexicon.journal")
    }
}
