//! A prefix's top three words, on every keystroke.
//!
//! No allocation and no error path: the file passed `validate`, so every read
//! below succeeds, and each still goes through `Option` so one that somehow did
//! not returns nothing rather than panicking.
//!
//! 1. Binary search the block index for the block the prefix falls in, then
//!    scan at most 16 words to the first whose key is not below the prefix.
//! 2. If fewer than four words from there carry the prefix, they are the answer.
//! 3. Otherwise the prefix is heavy, and its top three are stored: binary search
//!    the Heavy section for `(first word, prefix length)`.

use std::cmp::Ordering;
use std::ops::Range;

use super::Lexicon;
use super::format::sections::{block_at, heavy_at, word_at};
use super::format::{BLOCK, admissible, compare_keys, has_prefix};
use crate::index::TOP_K;
use crate::types::SuggestionSet;

impl Lexicon {
    pub(crate) fn top3(&self, prefix: &str) -> SuggestionSet<'_> {
        let mut set = SuggestionSet {
            items: [None; TOP_K],
            len: 0,
        };
        if admissible(prefix.as_bytes()) {
            self.fill(prefix.as_bytes(), &mut set);
        }
        set
    }

    /// Whether the list holds `word` itself, ignoring ASCII case.
    ///
    /// The same binary search the prefix path uses, stopped one step earlier: the
    /// first key not below the word either is the word or the word is not there.
    pub(crate) fn contains(&self, word: &str) -> bool {
        let key = word.as_bytes();
        admissible(key)
            && self
                .first_at_or_after(key)
                .and_then(|(_, offset)| word_at(self.words(), offset))
                .is_some_and(|(_, text, _)| compare_keys(text, key).is_eq())
    }

    fn fill<'a>(&'a self, prefix: &[u8], set: &mut SuggestionSet<'a>) -> Option<()> {
        let (first, offset) = self.first_at_or_after(prefix)?;
        // One more than the answer can hold: finding a fourth is what makes it heavy.
        let mut found = [(0u16, 0usize, &[] as &[u8]); TOP_K + 1];
        let (mut count, mut id, mut offset) = (0, first, offset);
        while count < found.len() && id < self.layout.word_count {
            let (rank, text, next) = word_at(self.words(), offset)?;
            if !has_prefix(text, prefix) {
                break;
            }
            found[count] = (rank, id, text);
            (count, id, offset) = (count + 1, id + 1, next);
        }
        if count <= TOP_K {
            let light = &mut found[..count];
            light.sort_unstable_by_key(|(rank, id, _)| (*rank, *id));
            light.iter().for_each(|(_, _, text)| push(set, text));
            return Some(());
        }
        let top = self.heavy_top(first, prefix.len())?;
        for id in top {
            push(set, self.word(usize::from(id))?);
        }
        Some(())
    }

    /// The first word whose key is not below `prefix`, as `(id, offset)`.
    fn first_at_or_after(&self, prefix: &[u8]) -> Option<(usize, usize)> {
        let blocks = self.layout.word_count.div_ceil(BLOCK);
        let first_of =
            |block: usize| Some(word_at(self.words(), block_at(self.blocks(), block)?)?.1);
        let (mut low, mut high) = (0, blocks);
        while low < high {
            let middle = low + (high - low) / 2;
            if compare_keys(first_of(middle)?, prefix).is_lt() {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        // Every block from `low` on starts at or after the prefix, so the answer
        // is in the block before it, or is its first word.
        let block = low.saturating_sub(1);
        let (mut id, mut offset) = (block * BLOCK, block_at(self.blocks(), block)?);
        while id < self.layout.word_count {
            let (_, text, next) = word_at(self.words(), offset)?;
            if compare_keys(text, prefix).is_ge() {
                return Some((id, offset));
            }
            (id, offset) = (id + 1, next);
        }
        None
    }

    fn heavy_top(&self, first: usize, length: usize) -> Option<[u16; TOP_K]> {
        let wanted = (first, length);
        let (mut low, mut high) = (0, self.layout.heavy_count);
        while low < high {
            let middle = low + (high - low) / 2;
            let (lo, plen, top) = heavy_at(self.heavy(), middle)?;
            match (usize::from(lo), usize::from(plen)).cmp(&wanted) {
                Ordering::Less => low = middle + 1,
                Ordering::Greater => high = middle,
                Ordering::Equal => return Some(top),
            }
        }
        None
    }

    fn word(&self, id: usize) -> Option<&[u8]> {
        let mut offset = block_at(self.blocks(), id / BLOCK)?;
        for _ in 0..id % BLOCK {
            offset = word_at(self.words(), offset)?.2;
        }
        Some(word_at(self.words(), offset)?.1)
    }

    fn blocks(&self) -> &[u8] {
        self.section(&self.layout.blocks)
    }

    fn words(&self) -> &[u8] {
        self.section(&self.layout.words)
    }

    fn heavy(&self) -> &[u8] {
        self.section(&self.layout.heavy)
    }

    fn section(&self, range: &Range<usize>) -> &[u8] {
        self.bytes.get(range.clone()).unwrap_or_default()
    }
}

/// Validated words are ASCII, so this cannot drop one; it just never trusts that.
fn push<'a>(set: &mut SuggestionSet<'a>, text: &'a [u8]) {
    if let (Ok(text), Some(slot)) = (std::str::from_utf8(text), set.items.get_mut(set.len)) {
        *slot = Some(text);
        set.len += 1;
    }
}
