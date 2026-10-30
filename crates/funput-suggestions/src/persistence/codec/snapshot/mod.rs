//! The snapshot record: the whole store in one checksummed frame.
//!
//! `magic · version · sequence · word count · words · [blocked] · crc32`
//!
//! - `words` — one word record, and the parts each version added to it.
//! - `blocked` — the blocked list that follows the words from v3.
//!
//! v1 is the word list alone. v2 appends each word's context count and follower
//! edges. v3 adds each word's origin and the blocked list.

mod blocked;
mod words;

use std::io;

use crate::binary::{Cursor, checksum, put_u16, put_u32, put_u64};
use crate::persistence::schema::{self, SNAPSHOT_MAGIC, SNAPSHOT_WRITE_VERSION, Version};
use crate::types::WordRecord;

/// What a snapshot holds once read.
#[derive(Default)]
pub(crate) struct Snapshot {
    pub(crate) words: Vec<WordRecord>,
    pub(crate) blocked: Vec<String>,
    pub(crate) sequence: u64,
}

pub(crate) fn encode(words: &[WordRecord], blocked: &[String], sequence: u64) -> Vec<u8> {
    let mut bytes = Vec::new();
    bytes.extend_from_slice(SNAPSHOT_MAGIC);
    put_u16(&mut bytes, SNAPSHOT_WRITE_VERSION);
    put_u64(&mut bytes, sequence);
    put_u32(&mut bytes, words.len() as u32);
    for word in words {
        words::encode(&mut bytes, word);
    }
    blocked::encode(&mut bytes, blocked);
    let sum = checksum(&bytes);
    put_u32(&mut bytes, sum);
    bytes
}

/// `Ok(None)` is a file to throw away and rebuild; an error is one to leave alone.
pub(crate) fn decode(bytes: &[u8]) -> io::Result<Option<Snapshot>> {
    if bytes.len() < SNAPSHOT_MAGIC.len() + 2 + 8 + 4 + 4 {
        return Ok(None);
    }
    let (content, checksum_bytes) = bytes.split_at(bytes.len() - 4);
    if checksum(content) != Cursor::new(checksum_bytes).u32()? {
        return Ok(None);
    }
    let mut cursor = Cursor::new(content);
    if cursor.take(SNAPSHOT_MAGIC.len())? != SNAPSHOT_MAGIC {
        return Ok(None);
    }
    let version = cursor.u16()?;
    match schema::accept_snapshot(version) {
        Version::Readable => {}
        Version::TooNew => {
            return Err(io::Error::new(
                io::ErrorKind::Unsupported,
                "personal lexicon schema is newer than this build",
            ));
        }
        Version::TooOld => return Ok(None),
    }
    let sequence = cursor.u64()?;
    let count = cursor.u32()? as usize;
    if count > cursor.remaining() / words::MIN_RECORD_BYTES {
        return Ok(None);
    }
    let mut records = Vec::with_capacity(count);
    for _ in 0..count {
        records.push(words::decode(&mut cursor, version)?);
    }
    let blocked = if version >= 3 {
        blocked::decode(&mut cursor)?
    } else {
        Vec::new()
    };
    Ok(cursor.is_empty().then_some(Snapshot {
        words: records,
        blocked,
        sequence,
    }))
}
