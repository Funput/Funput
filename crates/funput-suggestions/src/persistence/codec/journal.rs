//! The journal record: one checksummed frame per batch of learned tokens.
//!
//! The frame is already in typing order, so a pair is simply two adjacent
//! tokens and the pairs themselves never need writing. What each token carries
//! is one byte saying whether it really did follow the token before it.
//!
//! v1 has no such byte and recorded no boundaries at all, so nothing in a v1
//! frame can be believed as adjacent.

use std::io;

use super::binary::{Cursor, checksum, invalid_data, put_u16, put_u32};
use crate::persistence::schema::{self, JOURNAL_MAGIC, JOURNAL_WRITE_VERSION, Version};

/// One learned token, and whether it followed the token written before it.
pub(crate) type Entry = (String, bool);

pub(crate) fn encode_frame(entries: &[Entry]) -> Vec<u8> {
    let mut payload = Vec::new();
    put_u32(&mut payload, entries.len() as u32);
    for (token, chained) in entries {
        put_u16(&mut payload, token.len() as u16);
        payload.push(u8::from(*chained));
        payload.extend_from_slice(token.as_bytes());
    }
    let mut frame = Vec::with_capacity(14 + payload.len());
    frame.extend_from_slice(JOURNAL_MAGIC);
    put_u16(&mut frame, JOURNAL_WRITE_VERSION);
    put_u32(&mut frame, payload.len() as u32);
    put_u32(&mut frame, checksum(&payload));
    frame.extend_from_slice(&payload);
    frame
}

pub(crate) fn decode_frame(cursor: &mut Cursor<'_>) -> io::Result<Vec<Entry>> {
    if cursor.take(JOURNAL_MAGIC.len())? != JOURNAL_MAGIC {
        return Err(invalid_data());
    }
    let version = cursor.u16()?;
    match schema::accept_journal(version) {
        Version::Readable => {}
        Version::TooNew => {
            return Err(io::Error::new(
                io::ErrorKind::Unsupported,
                "journal schema is newer than this build",
            ));
        }
        // Not fatal: the reader stops at this frame and keeps the prefix it
        // already decoded, the same as it does for a torn one.
        Version::TooOld => return Err(invalid_data()),
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
    let mut entries = Vec::with_capacity(count);
    for _ in 0..count {
        let len = payload.u16()? as usize;
        // A v1 frame has no flag byte, and nothing in it may be read as adjacent.
        let chained = version >= 2 && payload.u8()? != 0;
        let token = std::str::from_utf8(payload.take(len)?)
            .map_err(|_| invalid_data())?
            .to_owned();
        entries.push((token, chained));
    }
    if !payload.is_empty() {
        return Err(invalid_data());
    }
    Ok(entries)
}
