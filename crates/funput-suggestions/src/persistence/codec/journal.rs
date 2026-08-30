//! The journal record: one checksummed frame per batch of learned tokens.

use std::io;

use super::binary::{Cursor, checksum, invalid_data, put_u16, put_u32};
use crate::persistence::schema::{JOURNAL_MAGIC, VERSION};

pub(crate) fn encode_frame(tokens: &[String]) -> Vec<u8> {
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
    put_u32(&mut frame, checksum(&payload));
    frame.extend_from_slice(&payload);
    frame
}

pub(crate) fn decode_frame(cursor: &mut Cursor<'_>) -> io::Result<Vec<String>> {
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
