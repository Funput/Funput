//! The snapshot record: the whole word list in one checksummed frame.
//!
//! v1 is the word list alone. v2 appends each word's context count and its
//! follower edges. The generation is never written: it means something only
//! within one run, and every record comes back at zero, so the edges written
//! against it come back matching.

use std::io;

use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::types::WordRecord;

use super::binary::{Cursor, checksum, invalid_data, put_u16, put_u32, put_u64};
use crate::persistence::schema::{self, SNAPSHOT_MAGIC, SNAPSHOT_WRITE_VERSION, Version};

pub(crate) fn encode(words: &[WordRecord], sequence: u64) -> Vec<u8> {
    let mut bytes = Vec::new();
    bytes.extend_from_slice(SNAPSHOT_MAGIC);
    put_u16(&mut bytes, SNAPSHOT_WRITE_VERSION);
    put_u64(&mut bytes, sequence);
    put_u32(&mut bytes, words.len() as u32);
    for word in words {
        put_u16(&mut bytes, word.text.len() as u16);
        bytes.extend_from_slice(word.text.as_bytes());
        put_u32(&mut bytes, word.uses);
        put_u64(&mut bytes, word.last_used);
        encode_followers(&mut bytes, word);
    }
    let sum = checksum(&bytes);
    put_u32(&mut bytes, sum);
    bytes
}

/// Only the occupied slots go out, so a word nobody has followed costs three
/// bytes rather than twenty-seven.
fn encode_followers(bytes: &mut Vec<u8>, word: &WordRecord) {
    put_u16(bytes, word.context_seen);
    let live = word.followers.iter().filter(|slot| !slot.is_free());
    bytes.push(live.clone().count() as u8);
    for follower in live {
        put_u32(bytes, follower.word);
        put_u16(bytes, follower.uses);
    }
}

fn decode_followers(cursor: &mut Cursor<'_>) -> io::Result<(u16, [Follower; FOLLOWER_SLOTS])> {
    let context_seen = cursor.u16()?;
    let count = cursor.u8()? as usize;
    if count > FOLLOWER_SLOTS {
        return Err(invalid_data());
    }
    let mut followers = [Follower::EMPTY; FOLLOWER_SLOTS];
    for slot in followers.iter_mut().take(count) {
        // Generation zero to match the records this file is being read into. An
        // id past the end of the list needs no check of its own: `Follower::word`
        // reads it as dead, and the checksum has already ruled out real damage.
        *slot = Follower {
            word: cursor.u32()?,
            generation: 0,
            uses: cursor.u16()?,
        };
    }
    Ok((context_seen, followers))
}

pub(crate) fn decode(bytes: &[u8]) -> io::Result<Option<(Vec<WordRecord>, u64)>> {
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
    if count > cursor.remaining() / 14 {
        return Ok(None);
    }
    let mut words = Vec::with_capacity(count);
    for _ in 0..count {
        let len = cursor.u16()? as usize;
        let text = std::str::from_utf8(cursor.take(len)?)
            .map_err(|_| invalid_data())?
            .to_owned();
        let uses = cursor.u32()?;
        let last_used = cursor.u64()?;
        let (context_seen, followers) = if version >= 2 {
            decode_followers(&mut cursor)?
        } else {
            (0, [Follower::EMPTY; FOLLOWER_SLOTS])
        };
        words.push(WordRecord {
            text,
            uses,
            last_used,
            // Never serialized: a generation means something only inside the run
            // that issued it, and starting everyone at zero is what makes the
            // edges above line up again.
            generation: 0,
            context_seen,
            followers,
        });
    }
    Ok(cursor.is_empty().then_some((words, sequence)))
}
