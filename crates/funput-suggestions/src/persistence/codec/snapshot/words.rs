//! One word record of the snapshot.
//!
//! `u16 len · text · u32 uses · u64 last_used · [v3: u8 flags] · [v2: edges]`
//!
//! The generation is never written: it means something only within one run, and
//! every record comes back at zero, so the edges written against it come back
//! matching. That is why only live edges go out: a dead one names a slot that
//! another word has since taken, and at generation zero it would point at it.

use std::io;

use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::binary::{Cursor, invalid_data, put_u16, put_u32, put_u64};
use crate::dictionary::Origin;
use crate::types::WordRecord;

/// The smallest record any version writes: an empty text and the two counters.
/// What a word count is checked against before anything is allocated for it.
pub(super) const MIN_RECORD_BYTES: usize = 2 + 4 + 8;

/// Bit 0 of the v3 flags byte: the word was added by hand. Bits nobody has
/// defined yet are ignored on read, so a later flag needs no version of its own.
const ADDED: u8 = 1;

pub(super) fn encode(bytes: &mut Vec<u8>, word: &WordRecord, words: &[WordRecord]) {
    put_u16(bytes, word.text.len() as u16);
    bytes.extend_from_slice(word.text.as_bytes());
    put_u32(bytes, word.uses);
    put_u64(bytes, word.last_used);
    bytes.push(if word.origin == Origin::Added {
        ADDED
    } else {
        0
    });
    encode_edges(bytes, word.context_seen, &word.followers, words);
}

pub(super) fn decode(cursor: &mut Cursor<'_>, version: u16) -> io::Result<WordRecord> {
    let len = cursor.u16()? as usize;
    let text = std::str::from_utf8(cursor.take(len)?)
        .map_err(|_| invalid_data())?
        .to_owned();
    let mut word = WordRecord::new(text, cursor.u32()?, cursor.u64()?);
    if version >= 3 && cursor.u8()? & ADDED != 0 {
        word.origin = Origin::Added;
    }
    if version >= 2 {
        (word.context_seen, word.followers) = decode_edges(cursor)?;
    }
    Ok(word)
}

/// A context's count and its follower edges, for a word here and for a trigram
/// entry in `ngram`.
///
/// Only the slots still pointing at their word go out, so a word nobody has
/// followed costs three bytes rather than twenty-seven.
pub(super) fn encode_edges(
    bytes: &mut Vec<u8>,
    context_seen: u16,
    followers: &[Follower; FOLLOWER_SLOTS],
    words: &[WordRecord],
) {
    put_u16(bytes, context_seen);
    let live = followers.iter().filter(|slot| slot.word(words).is_some());
    bytes.push(live.clone().count() as u8);
    for follower in live {
        put_u32(bytes, follower.word);
        put_u16(bytes, follower.uses);
    }
}

pub(super) fn decode_edges(
    cursor: &mut Cursor<'_>,
) -> io::Result<(u16, [Follower; FOLLOWER_SLOTS])> {
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
