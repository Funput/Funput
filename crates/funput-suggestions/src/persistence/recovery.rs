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
use super::codec::binary::{Cursor, invalid_data};
use super::codec::{journal, snapshot};
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

    pub(super) fn load_journal(&self) -> io::Result<(Vec<String>, u64)> {
        let path = self.journal_path();
        let bytes = match fs::metadata(&path) {
            Ok(metadata) if metadata.len() > MAX_JOURNAL_BYTES => return Err(invalid_data()),
            Ok(_) => fs::read(path)?,
            Err(error) if error.kind() == io::ErrorKind::NotFound => return Ok((Vec::new(), 0)),
            Err(error) => return Err(error),
        };
        let journal_bytes = bytes.len() as u64;
        let mut cursor = Cursor::new(&bytes);
        let mut tokens = Vec::new();
        while !cursor.is_empty() {
            match journal::decode_frame(&mut cursor) {
                Ok(frame) => tokens.extend(frame),
                Err(error) if error.kind() == io::ErrorKind::Unsupported => return Err(error),
                Err(_) => break,
            }
        }
        Ok((tokens, journal_bytes))
    }
}
