//! What an `en.lex` file is allowed to look like.
//!
//! A fixed header, then three sections, all little-endian:
//!
//! ```text
//! Header  magic "FPLX" · version u16 · flags u16 · word_count u32
//!         words_len u32 · heavy_count u32 · body_crc u32               24 bytes
//! Blocks  ceil(word_count / 16) × u32   where every 16th word starts in Words
//! Words   word_count × { len u8 · rank u16 · bytes }   sorted by lowercase key
//! Heavy   heavy_count × { lo u16 · plen u8 · top [u16; 3] }   sorted by (lo, plen)
//! ```
//!
//! A prefix matching three words or fewer needs nothing stored: those words are
//! the answer. Only a prefix matching more — a *heavy* one — gets its top three
//! written down. `body_crc` covers everything after the header and doubles as
//! the file's identity, so a shell can tell two builds of the data apart without
//! anyone remembering to bump a number.

use std::cmp::Ordering;
use std::io;

use crate::binary::{Cursor, invalid_data, put_u16, put_u32};
use crate::engine::MAX_TOKEN_SCALARS;

pub(crate) const MAGIC: &[u8; 4] = b"FPLX";
pub(crate) const VERSION: u16 = 1;
pub(crate) const HEADER_BYTES: usize = 24;
/// Words per block-index entry: the most a lookup ever scans past a jump.
pub(crate) const BLOCK: usize = 16;
/// `len u8 · rank u16`, ahead of each word's bytes.
pub(crate) const WORD_HEAD_BYTES: usize = 3;
pub(crate) const HEAVY_BYTES: usize = 9;
/// Ids and `lo` are `u16`.
pub(crate) const MAX_WORDS: usize = u16::MAX as usize;
/// The shells ask for nothing shorter.
pub(crate) const MIN_LEN: usize = 2;
pub(crate) const MAX_LEN: usize = MAX_TOKEN_SCALARS;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) struct Header {
    pub(crate) version: u16,
    pub(crate) flags: u16,
    pub(crate) word_count: u32,
    pub(crate) words_len: u32,
    pub(crate) heavy_count: u32,
    pub(crate) body_crc: u32,
}

impl Header {
    /// The fields as written. Whether they describe a file worth reading is
    /// `validate`'s question, not this one's.
    pub(crate) fn parse(bytes: &[u8]) -> io::Result<Self> {
        let mut cursor = Cursor::new(bytes);
        if cursor.take(MAGIC.len())? != MAGIC {
            return Err(invalid_data());
        }
        Ok(Self {
            version: cursor.u16()?,
            flags: cursor.u16()?,
            word_count: cursor.u32()?,
            words_len: cursor.u32()?,
            heavy_count: cursor.u32()?,
            body_crc: cursor.u32()?,
        })
    }

    pub(crate) fn write(&self, out: &mut Vec<u8>) {
        out.extend_from_slice(MAGIC);
        put_u16(out, self.version);
        put_u16(out, self.flags);
        put_u32(out, self.word_count);
        put_u32(out, self.words_len);
        put_u32(out, self.heavy_count);
        put_u32(out, self.body_crc);
    }
}

pub(crate) fn block_count(word_count: usize) -> usize {
    word_count.div_ceil(BLOCK)
}

/// Whether the lexicon can hold `word`: plain ASCII letters, two to 32 of them.
/// Everything the lookup compares relies on this, so both ends check it.
pub(crate) fn admissible(word: &[u8]) -> bool {
    (MIN_LEN..=MAX_LEN).contains(&word.len()) && word.iter().all(u8::is_ascii_alphabetic)
}

/// The order words are stored in: byte order of the lowercased word. No
/// allocation, because the lookup compares with it on every keystroke.
pub(crate) fn compare_keys(left: &[u8], right: &[u8]) -> Ordering {
    left.iter()
        .map(u8::to_ascii_lowercase)
        .cmp(right.iter().map(u8::to_ascii_lowercase))
}

/// Whether `word` starts with `prefix`, ignoring ASCII case.
pub(crate) fn has_prefix(word: &[u8], prefix: &[u8]) -> bool {
    word.get(..prefix.len())
        .is_some_and(|head| head.eq_ignore_ascii_case(prefix))
}
