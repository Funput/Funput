//! The blocked list, written after the words from v3: `u32 count · (u16 len · text)*`.

use std::io;

use crate::binary::{Cursor, invalid_data, put_u16, put_u32};

pub(super) fn encode(bytes: &mut Vec<u8>, blocked: &[String]) {
    put_u32(bytes, blocked.len() as u32);
    for word in blocked {
        put_u16(bytes, word.len() as u16);
        bytes.extend_from_slice(word.as_bytes());
    }
}

pub(super) fn decode(cursor: &mut Cursor<'_>) -> io::Result<Vec<String>> {
    let count = cursor.u32()? as usize;
    if count > cursor.remaining() / 2 {
        return Err(invalid_data());
    }
    let mut blocked = Vec::with_capacity(count);
    for _ in 0..count {
        let len = cursor.u16()? as usize;
        let word = std::str::from_utf8(cursor.take(len)?).map_err(|_| invalid_data())?;
        blocked.push(word.to_owned());
    }
    Ok(blocked)
}
