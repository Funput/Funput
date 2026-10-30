//! An edit file: the edits one tap in the app made, for the keyboard to apply.
//!
//! `magic · u16 version · u32 payload length · u32 crc32 · payload`, where the
//! payload is `u32 count` followed by that many [`Record::Edit`] entries. A file is
//! one frame and nothing after it: anything else is damage, not a second frame.

use std::io;

use super::record::{Record, TagFormat};
use crate::binary::{Cursor, checksum, invalid_data, put_u16, put_u32};
use crate::dictionary::Edit;
use crate::persistence::schema::{self, INBOX_MAGIC, INBOX_WRITE_VERSION, Version};

pub(crate) fn encode(edits: &[Edit<'_>]) -> Vec<u8> {
    let mut payload = Vec::new();
    put_u32(&mut payload, edits.len() as u32);
    for edit in edits {
        let record = Record::Edit {
            kind: edit.kind,
            word: edit.word.to_owned(),
        };
        record.encode(&mut payload);
    }
    let mut bytes = Vec::with_capacity(14 + payload.len());
    bytes.extend_from_slice(INBOX_MAGIC);
    put_u16(&mut bytes, INBOX_WRITE_VERSION);
    put_u32(&mut bytes, payload.len() as u32);
    put_u32(&mut bytes, checksum(&payload));
    bytes.extend_from_slice(&payload);
    bytes
}

/// Every entry is a [`Record::Edit`]; a file holding anything else is damaged.
/// A file from a newer build is `Unsupported`, which the reader leaves in place.
pub(crate) fn decode(bytes: &[u8]) -> io::Result<Vec<Record>> {
    let mut cursor = Cursor::new(bytes);
    if cursor.take(INBOX_MAGIC.len())? != INBOX_MAGIC {
        return Err(invalid_data());
    }
    match schema::accept_inbox(cursor.u16()?) {
        Version::Readable => {}
        Version::TooNew => {
            return Err(io::Error::new(
                io::ErrorKind::Unsupported,
                "edit file is newer than this build",
            ));
        }
        Version::TooOld => return Err(invalid_data()),
    }
    let payload_len = cursor.u32()? as usize;
    let expected = cursor.u32()?;
    let payload = cursor.take(payload_len)?;
    if checksum(payload) != expected || !cursor.is_empty() {
        return Err(invalid_data());
    }
    let mut payload = Cursor::new(payload);
    let count = payload.u32()? as usize;
    if count > payload.remaining() / 3 {
        return Err(invalid_data());
    }
    let mut records = Vec::with_capacity(count);
    for _ in 0..count {
        match Record::decode(&mut payload, TagFormat::Tagged)? {
            record @ Record::Edit { .. } => records.push(record),
            Record::Learned { .. } => return Err(invalid_data()),
        }
    }
    if !payload.is_empty() {
        return Err(invalid_data());
    }
    Ok(records)
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dictionary::EditKind;

    #[test]
    fn a_file_round_trips_and_rejects_any_damage() {
        let edits = [
            Edit::new(EditKind::Add, "Funput"),
            Edit::new(EditKind::Block, "đm"),
        ];
        let bytes = encode(&edits);
        let records = decode(&bytes).ok().unwrap_or_default();
        assert_eq!(records.len(), 2);
        assert_eq!(
            records[1],
            Record::Edit {
                kind: EditKind::Block,
                word: "đm".into()
            }
        );

        let mut torn = bytes.clone();
        torn.pop();
        assert!(decode(&torn).is_err());
        let mut trailing = bytes.clone();
        trailing.push(0);
        assert!(decode(&trailing).is_err());
    }

    #[test]
    fn a_newer_file_is_left_for_a_newer_build() {
        let mut bytes = encode(&[Edit::new(EditKind::Forget, "a")]);
        bytes[4..6].copy_from_slice(&(INBOX_WRITE_VERSION + 1).to_le_bytes());
        let error = decode(&bytes).err().map(|error| error.kind());
        assert_eq!(error, Some(io::ErrorKind::Unsupported));
    }
}
