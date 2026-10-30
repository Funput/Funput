//! An edit file: the edits one post from the app made, for the keyboard to apply.
//!
//! One [`super::frame`] whose payload is `u32 count` followed by that many
//! [`Record::Edit`] entries. A file is one frame and nothing after it: anything
//! else is damage, not a second frame.

use std::io;

use super::frame;
use super::record::{Record, TagFormat};
use crate::binary::{Cursor, invalid_data, put_u32};
use crate::dictionary::{Edit, EditKind};
use crate::persistence::schema::{self, INBOX_MAGIC, INBOX_WRITE_VERSION};

/// The bytes one entry can take at most, given a word the caller has already
/// bounded to `max_word_bytes`: the length, the tag, and the word.
pub(crate) const fn max_entry_bytes(max_word_bytes: usize) -> usize {
    2 + 1 + max_word_bytes
}

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
    frame::encode(INBOX_MAGIC, INBOX_WRITE_VERSION, &payload)
}

/// A file holding anything but edits is damaged. A file from a newer build is
/// `Unsupported`, which the reader leaves in place.
pub(crate) fn decode(bytes: &[u8]) -> io::Result<Vec<(EditKind, String)>> {
    let mut cursor = Cursor::new(bytes);
    let (_, payload) = frame::decode(&mut cursor, INBOX_MAGIC, schema::accept_inbox)?;
    if !cursor.is_empty() {
        return Err(invalid_data());
    }
    let mut payload = Cursor::new(payload);
    let count = payload.u32()? as usize;
    if count > payload.remaining() / max_entry_bytes(0) {
        return Err(invalid_data());
    }
    let mut edits = Vec::with_capacity(count);
    for _ in 0..count {
        match Record::decode(&mut payload, TagFormat::Tagged)? {
            Record::Edit { kind, word } => edits.push((kind, word)),
            Record::Learned { .. } => return Err(invalid_data()),
        }
    }
    if !payload.is_empty() {
        return Err(invalid_data());
    }
    Ok(edits)
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn a_file_round_trips_and_rejects_any_damage() {
        let edits = [
            Edit::new(EditKind::Add, "Funput"),
            Edit::new(EditKind::Block, "đm"),
        ];
        let bytes = encode(&edits);
        let edits = decode(&bytes).ok().unwrap_or_default();
        assert_eq!(edits.len(), 2);
        assert_eq!(edits[1], (EditKind::Block, "đm".to_owned()));

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
