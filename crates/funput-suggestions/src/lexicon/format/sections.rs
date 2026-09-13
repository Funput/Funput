//! Reading the sections of an `en.lex` by position.
//!
//! Shared by `validate`, which walks every entry once, and by the lookup, which
//! reads a handful per keystroke. So every accessor returns `Option`, reads with
//! `get`, and allocates nothing: a file that passed validation cannot make them
//! fail, and one that somehow did not still cannot make them panic.

use std::ops::Range;

use super::{HEADER_BYTES, HEAVY_BYTES, Header, MAX_WORDS, WORD_HEAD_BYTES, block_count};
use crate::index::TOP_K;

/// Where each section sits in the file, derived from the header alone.
#[derive(Debug, Clone, PartialEq, Eq)]
pub(crate) struct Layout {
    pub(crate) word_count: usize,
    pub(crate) heavy_count: usize,
    pub(crate) blocks: Range<usize>,
    pub(crate) words: Range<usize>,
    pub(crate) heavy: Range<usize>,
}

impl Layout {
    /// `None` when the counts cannot describe a file: too many words for a
    /// `u16` id, or sizes that overflow.
    pub(crate) fn of(header: &Header) -> Option<Self> {
        let word_count = usize::try_from(header.word_count)
            .ok()
            .filter(|count| *count <= MAX_WORDS)?;
        let heavy_count = usize::try_from(header.heavy_count).ok()?;
        let blocks_end = HEADER_BYTES.checked_add(block_count(word_count).checked_mul(4)?)?;
        let words_end = blocks_end.checked_add(usize::try_from(header.words_len).ok()?)?;
        let heavy_end = words_end.checked_add(heavy_count.checked_mul(HEAVY_BYTES)?)?;
        Some(Self {
            word_count,
            heavy_count,
            blocks: HEADER_BYTES..blocks_end,
            words: blocks_end..words_end,
            heavy: words_end..heavy_end,
        })
    }
}

/// Where block `index` starts in the Words section.
pub(crate) fn block_at(blocks: &[u8], index: usize) -> Option<usize> {
    let at = index.checked_mul(4)?;
    let bytes = blocks.get(at..at.checked_add(4)?)?;
    usize::try_from(u32::from_le_bytes(bytes.try_into().ok()?)).ok()
}

/// The word starting at `offset` in the Words section: its rank, its bytes, and
/// the offset of the word after it.
pub(crate) fn word_at(words: &[u8], offset: usize) -> Option<(u16, &[u8], usize)> {
    let start = offset.checked_add(WORD_HEAD_BYTES)?;
    let &[len, low, high] = words.get(offset..start)? else {
        return None;
    };
    let end = start + usize::from(len);
    Some((u16::from_le_bytes([low, high]), words.get(start..end)?, end))
}

/// Heavy entry `index`: the first word of the run, the prefix length, and the
/// run's best three ids by rank.
pub(crate) fn heavy_at(heavy: &[u8], index: usize) -> Option<(u16, u8, [u16; TOP_K])> {
    let at = index.checked_mul(HEAVY_BYTES)?;
    let &[lo_low, lo_high, plen, a, b, c, d, e, f] = heavy.get(at..at.checked_add(HEAVY_BYTES)?)?
    else {
        return None;
    };
    let top = [[a, b], [c, d], [e, f]].map(u16::from_le_bytes);
    Some((u16::from_le_bytes([lo_low, lo_high]), plen, top))
}
