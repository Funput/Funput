//! The arena node the prefix trie is built from, the entry it remembers words by,
//! and the two constants that size it.

use crate::types::WordRecord;

pub(crate) const NONE: u32 = u32::MAX;
pub(crate) const TOP_K: usize = 3;

/// A word remembered by a node, tagged with the generation of the slot it was
/// read from. The tag is what makes an entry falsifiable: the engine reuses word
/// slots, and without it a node would keep answering with whatever word moved
/// into the index it cached.
#[derive(Debug, Clone, Copy)]
pub(super) struct Entry {
    pub(super) id: u32,
    pub(super) generation: u16,
}

impl Entry {
    pub(super) const EMPTY: Self = Self {
        id: NONE,
        generation: 0,
    };

    pub(super) fn of(id: u32, words: &[WordRecord]) -> Option<Self> {
        words.get(id as usize).map(|word| Self {
            id,
            generation: word.generation,
        })
    }

    /// The word this entry still refers to, if it refers to one at all.
    ///
    /// `get` rather than indexing on purpose: an entry can outlive the slot
    /// entirely, and a dangling id must read as dead rather than panic.
    pub(super) fn word<'a>(&self, words: &'a [WordRecord]) -> Option<&'a WordRecord> {
        words
            .get(self.id as usize)
            .filter(|word| word.generation == self.generation)
    }

    pub(super) fn live_id(&self, words: &[WordRecord]) -> u32 {
        if self.word(words).is_some() {
            self.id
        } else {
            NONE
        }
    }
}

#[derive(Debug, Clone)]
pub(super) struct Node {
    pub(super) label: char,
    pub(super) first_child: u32,
    pub(super) next_sibling: u32,
    pub(super) top: [Entry; TOP_K],
}

impl Node {
    pub(super) fn new(label: char) -> Self {
        Self {
            label,
            first_child: NONE,
            next_sibling: NONE,
            top: [Entry::EMPTY; TOP_K],
        }
    }
}
