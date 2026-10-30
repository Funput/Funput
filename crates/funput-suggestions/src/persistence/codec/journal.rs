//! The journal record: one checksummed frame per batch of learned tokens and
//! dictionary edits.
//!
//! The frame is already in typing order, so a pair is simply two adjacent
//! tokens and the pairs themselves never need writing. What each token carries
//! is one byte saying whether it really did follow the token before it.
//!
//! v1 has no such byte and recorded no boundaries at all, so nothing in a v1
//! frame can be believed as adjacent. v4 widens the byte into a tag, so a frame
//! can also carry the edits a person made; see [`super::record`].
//!
//! From v3 a frame also records the engine sequence its last token was learned
//! at. `compact` folds the journal into the snapshot and then truncates it, and
//! those are two writes rather than one: a crash in between leaves a snapshot
//! that already contains the frame next to a journal that still holds it. The
//! sequence is what lets the reader tell, so replaying such a journal adds
//! nothing rather than counting every token in it twice.

use std::io;

use super::frame;
use super::record::{Record, TagFormat};
use crate::binary::{Cursor, invalid_data, put_u32, put_u64};
use crate::persistence::schema::{self, JOURNAL_MAGIC, JOURNAL_WRITE_VERSION};

/// A decoded frame: its entries, and the sequence it ends at when the format
/// recorded one. `None` is a frame from before v3, which has to be replayed
/// because nothing in it says whether it already has been.
pub(crate) struct Frame {
    pub(crate) sequence: Option<u64>,
    pub(crate) entries: Vec<Record>,
}

pub(crate) fn encode_frame(entries: &[Record], sequence: u64) -> Vec<u8> {
    let mut payload = Vec::new();
    put_u64(&mut payload, sequence);
    put_u32(&mut payload, entries.len() as u32);
    for entry in entries {
        entry.encode(&mut payload);
    }
    frame::encode(JOURNAL_MAGIC, JOURNAL_WRITE_VERSION, &payload)
}

/// An `InvalidData` error is not fatal: the reader stops at this frame and keeps
/// the prefix it already decoded, the same as it does for a torn one.
pub(crate) fn decode_frame(cursor: &mut Cursor<'_>) -> io::Result<Frame> {
    let (version, payload) = frame::decode(cursor, JOURNAL_MAGIC, schema::accept_journal)?;
    let mut payload = Cursor::new(payload);
    let sequence = if version >= 3 {
        Some(payload.u64()?)
    } else {
        None
    };
    let count = payload.u32()? as usize;
    if count > payload.remaining() / 2 {
        return Err(invalid_data());
    }
    let format = match version {
        1 => TagFormat::Absent,
        2 | 3 => TagFormat::Flag,
        _ => TagFormat::Tagged,
    };
    let mut entries = Vec::with_capacity(count);
    for _ in 0..count {
        entries.push(Record::decode(&mut payload, format)?);
    }
    if !payload.is_empty() {
        return Err(invalid_data());
    }
    Ok(Frame { sequence, entries })
}
