//! Reading a store that may be damaged.
//!
//! Every branch here answers one question: is this file something we can still
//! read, something we should throw away and rebuild, or something we must not
//! touch? Keeping that judgement in one module is what stops each call site from
//! inventing its own answer.

use std::fs::{self, File};
use std::io::{self, Read};

use crate::types::WordRecord;

use super::Store;
use super::codec::binary::Cursor;
use super::codec::{journal, snapshot};
use super::fs::{sync_dir, truncate};
use super::schema::{MAX_JOURNAL_BYTES, MAX_SNAPSHOT_BYTES};

impl Store {
    pub(super) fn load_snapshot(&self) -> io::Result<Option<(Vec<WordRecord>, u64)>> {
        let mut bytes = Vec::new();
        match File::open(self.snapshot_path()) {
            Ok(mut file) => {
                if file.metadata()?.len() > MAX_SNAPSHOT_BYTES {
                    return Ok(None);
                }
                file.read_to_end(&mut bytes)?;
            }
            Err(error) if error.kind() == io::ErrorKind::NotFound => {
                return Ok(Some((Vec::new(), 0)));
            }
            Err(error) => return Err(error),
        }
        snapshot::decode(&bytes)
    }

    pub(super) fn load_journal(&self) -> io::Result<(Vec<super::JournalEntry>, u64)> {
        let path = self.journal_path();
        let bytes = match fs::metadata(&path) {
            // Far past the 64 KiB that triggers a compact, so this is not
            // something we wrote. Dropping it costs the tokens since the last
            // snapshot; returning an error costs the shells their persistence for
            // the rest of the process.
            Ok(metadata) if metadata.len() > MAX_JOURNAL_BYTES => {
                self.discard_journal()?;
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
                Ok(frame) => entries.extend(frame),
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
    pub(super) fn discard_journal(&self) -> io::Result<()> {
        truncate(&self.journal_path())?;
        sync_dir(&self.root)
    }
}
