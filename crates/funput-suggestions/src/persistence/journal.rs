use std::fs;
use std::io;

use super::binary::{Cursor, checksum, invalid_data};
use super::{JOURNAL_MAGIC, MAX_JOURNAL_BYTES, Store, VERSION};

impl Store {
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
            match decode_frame(&mut cursor) {
                Ok(frame) => tokens.extend(frame),
                Err(error) if error.kind() == io::ErrorKind::Unsupported => return Err(error),
                Err(_) => break,
            }
        }
        Ok((tokens, journal_bytes))
    }
}

fn decode_frame(cursor: &mut Cursor<'_>) -> io::Result<Vec<String>> {
    if cursor.take(JOURNAL_MAGIC.len())? != JOURNAL_MAGIC {
        return Err(invalid_data());
    }
    if cursor.u16()? != VERSION {
        return Err(io::Error::new(io::ErrorKind::Unsupported, "journal schema"));
    }
    let payload_len = cursor.u32()? as usize;
    let expected = cursor.u32()?;
    let payload = cursor.take(payload_len)?;
    if checksum(payload) != expected {
        return Err(invalid_data());
    }
    let mut payload = Cursor::new(payload);
    let count = payload.u32()? as usize;
    if count > payload.remaining() / 2 {
        return Err(invalid_data());
    }
    let mut tokens = Vec::with_capacity(count);
    for _ in 0..count {
        let len = payload.u16()? as usize;
        tokens.push(
            std::str::from_utf8(payload.take(len)?)
                .map_err(|_| invalid_data())?
                .to_owned(),
        );
    }
    if !payload.is_empty() {
        return Err(invalid_data());
    }
    Ok(tokens)
}
