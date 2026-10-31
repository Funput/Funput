//! One context of the table, and what followed it.

use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::types::{WordRecord, WordRef};

#[derive(Debug, Clone, Copy)]
pub(crate) struct Entry<const K: usize> {
    /// The context, oldest word first.
    pub(crate) key: [WordRef; K],
    /// How often the context has been seen, as `WordRecord::context_seen`.
    pub(crate) context_seen: u16,
    /// Newcomers that wanted this entry's place since it was last seen. It yields
    /// once they match `context_seen`. Lives in what would be padding, so the
    /// entry stays 52 bytes, and is not written to disk.
    pub(crate) challenges: u16,
    pub(crate) followers: [Follower; FOLLOWER_SLOTS],
}

impl<const K: usize> Entry<K> {
    pub(crate) const EMPTY: Self = Self::new([WordRef::NONE; K]);

    pub(crate) const fn new(key: [WordRef; K]) -> Self {
        Self {
            key,
            context_seen: 0,
            challenges: 0,
            followers: [Follower::EMPTY; FOLLOWER_SLOTS],
        }
    }

    pub(crate) fn is_empty(&self) -> bool {
        self.key[0] == WordRef::NONE
    }

    /// Whether every word of the context is still the word it was written for.
    pub(crate) fn is_live(&self, words: &[WordRecord]) -> bool {
        !self.is_empty() && self.key.iter().all(|word| word.is_live(words))
    }
}
