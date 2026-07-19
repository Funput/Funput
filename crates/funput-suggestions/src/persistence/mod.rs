mod binary;
mod journal;
mod snapshot;

use std::fs::{self, File, OpenOptions};
use std::io::{self, Write};
use std::path::{Path, PathBuf};

use crate::types::WordRecord;
use binary::{checksum as crc32, put_u16, put_u32};

const SNAPSHOT_MAGIC: &[u8; 8] = b"FPSNAP01";
const JOURNAL_MAGIC: &[u8; 4] = b"FPJR";
const VERSION: u16 = 1;
const MAX_SNAPSHOT_BYTES: u64 = 2 * 1024 * 1024;
const MAX_JOURNAL_BYTES: u64 = 1024 * 1024;

pub(crate) struct Store {
    pub(super) root: PathBuf,
}

pub(crate) struct Loaded {
    pub(crate) words: Vec<WordRecord>,
    pub(crate) sequence: u64,
    pub(crate) journal: Vec<String>,
    pub(crate) journal_bytes: u64,
}

impl Store {
    pub(crate) fn open(root: &Path) -> io::Result<(Self, Loaded)> {
        fs::create_dir_all(root)?;
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

    pub(crate) fn append_journal(&self, tokens: &[String]) -> io::Result<u64> {
        if tokens.is_empty() {
            return Ok(0);
        }
        let mut payload = Vec::new();
        put_u32(&mut payload, tokens.len() as u32);
        for token in tokens {
            put_u16(&mut payload, token.len() as u16);
            payload.extend_from_slice(token.as_bytes());
        }
        let mut frame = Vec::with_capacity(14 + payload.len());
        frame.extend_from_slice(JOURNAL_MAGIC);
        put_u16(&mut frame, VERSION);
        put_u32(&mut frame, payload.len() as u32);
        put_u32(&mut frame, crc32(&payload));
        frame.extend_from_slice(&payload);

        let mut file = OpenOptions::new()
            .create(true)
            .append(true)
            .open(self.journal_path())?;
        file.write_all(&frame)?;
        file.sync_data()?;
        Ok(frame.len() as u64)
    }

    pub(crate) fn compact(&self, words: &[WordRecord], sequence: u64) -> io::Result<u64> {
        let bytes = snapshot::encode(words, sequence);
        let temporary = self.root.join("personal-lexicon.snapshot.tmp");
        let mut file = File::create(&temporary)?;
        file.write_all(&bytes)?;
        file.sync_all()?;
        fs::rename(&temporary, self.snapshot_path())?;
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

#[cfg(test)]
pub(crate) use binary::checksum;
