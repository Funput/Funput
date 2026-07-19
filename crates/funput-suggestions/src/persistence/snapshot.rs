use std::fs::File;
use std::io::{self, Read};

use crate::types::WordRecord;

use super::binary::{Cursor, checksum, invalid_data, put_u16, put_u32, put_u64};
use super::{MAX_SNAPSHOT_BYTES, SNAPSHOT_MAGIC, Store, VERSION};

pub(super) fn encode(words: &[WordRecord], sequence: u64) -> Vec<u8> {
    let mut bytes = Vec::new();
    bytes.extend_from_slice(SNAPSHOT_MAGIC);
    put_u16(&mut bytes, VERSION);
    put_u64(&mut bytes, sequence);
    put_u32(&mut bytes, words.len() as u32);
    for word in words {
        put_u16(&mut bytes, word.text.len() as u16);
        bytes.extend_from_slice(word.text.as_bytes());
        put_u32(&mut bytes, word.uses);
        put_u64(&mut bytes, word.last_used);
    }
    let sum = checksum(&bytes);
    put_u32(&mut bytes, sum);
    bytes
}

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
        decode(&bytes)
    }
}

fn decode(bytes: &[u8]) -> io::Result<Option<(Vec<WordRecord>, u64)>> {
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
    if cursor.u16()? != VERSION {
        return Err(io::Error::new(
            io::ErrorKind::Unsupported,
            "personal lexicon schema is newer or unsupported",
        ));
    }
    let sequence = cursor.u64()?;
    let count = cursor.u32()? as usize;
    if count > cursor.remaining() / 14 {
        return Ok(None);
    }
    let mut words = Vec::with_capacity(count);
    for _ in 0..count {
        let len = cursor.u16()? as usize;
        let text = std::str::from_utf8(cursor.take(len)?)
            .map_err(|_| invalid_data())?
            .to_owned();
        words.push(WordRecord {
            text,
            uses: cursor.u32()?,
            last_used: cursor.u64()?,
        });
    }
    Ok(cursor.is_empty().then_some((words, sequence)))
}
