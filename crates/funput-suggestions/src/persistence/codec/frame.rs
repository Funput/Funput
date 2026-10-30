//! The checksummed frame the journal and the edit files share:
//!
//! `magic · u16 version · u32 payload length · u32 crc32 · payload`
//!
//! Each format owns its magic, its version window and its payload; this owns how
//! they are wrapped and what a damaged or foreign wrapper means.

use std::io;

use crate::binary::{Cursor, checksum, invalid_data, put_u16, put_u32};
use crate::persistence::schema::Version;

const HEADER_BYTES: usize = 4 + 2 + 4 + 4;

pub(crate) fn encode(magic: &[u8; 4], version: u16, payload: &[u8]) -> Vec<u8> {
    let mut frame = Vec::with_capacity(HEADER_BYTES + payload.len());
    frame.extend_from_slice(magic);
    put_u16(&mut frame, version);
    put_u32(&mut frame, payload.len() as u32);
    put_u32(&mut frame, checksum(payload));
    frame.extend_from_slice(payload);
    frame
}

/// Reads one frame and returns its version and payload.
///
/// A version newer than `accept` allows is `Unsupported`: the build that wrote it
/// still understands it, so the caller must leave it alone. An older version, a
/// wrong magic, a short read or a bad checksum is `InvalidData` — damage.
pub(crate) fn decode<'a>(
    cursor: &mut Cursor<'a>,
    magic: &[u8; 4],
    accept: fn(u16) -> Version,
) -> io::Result<(u16, &'a [u8])> {
    if cursor.take(magic.len())? != magic {
        return Err(invalid_data());
    }
    let version = cursor.u16()?;
    match accept(version) {
        Version::Readable => {}
        Version::TooNew => {
            return Err(io::Error::new(
                io::ErrorKind::Unsupported,
                "suggestion data is newer than this build",
            ));
        }
        Version::TooOld => return Err(invalid_data()),
    }
    let payload_len = cursor.u32()? as usize;
    let expected = cursor.u32()?;
    let payload = cursor.take(payload_len)?;
    if checksum(payload) != expected {
        return Err(invalid_data());
    }
    Ok((version, payload))
}
