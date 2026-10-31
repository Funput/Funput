//! One word of a context, and how a context is hashed.

use crate::bigram::follower::Follower;
use crate::index::NONE;
use crate::types::WordRecord;

/// A word slot and the generation it was written at.
///
/// The same trap and the same answer as `Follower::word`: the engine hands word
/// slots to new words, so a reference only means anything while the generation
/// it was taken at still matches.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) struct WordRef {
    pub(crate) index: u32,
    pub(crate) generation: u16,
}

impl WordRef {
    pub(crate) const NONE: Self = Self {
        index: NONE,
        generation: 0,
    };

    pub(crate) fn at(index: u32, words: &[WordRecord]) -> Option<Self> {
        words.get(index as usize).map(|word| Self {
            index,
            generation: word.generation,
        })
    }

    pub(crate) fn is_live(self, words: &[WordRecord]) -> bool {
        words
            .get(self.index as usize)
            .is_some_and(|word| word.generation == self.generation)
    }

    /// An edge to this word, seen once.
    pub(crate) fn follower(self) -> Follower {
        Follower {
            word: self.index,
            generation: self.generation,
            uses: 1,
        }
    }

    /// Whether `edge` names this very slot and generation, which is how removal
    /// describes the words it moves.
    pub(crate) fn is(self, edge: Follower) -> bool {
        self.index == edge.word && self.generation == edge.generation
    }
}

/// Where a key starts probing. Multiplicative hashing on each word's slot and
/// generation: cheap, no allocation, and the generation keeps a reused slot from
/// landing on its old tenant's entries.
pub(crate) fn hash<const K: usize>(key: &[WordRef; K]) -> u64 {
    const SEED: u64 = 0x9e37_79b9_7f4a_7c15;
    key.iter().fold(0, |hash, word| {
        let value = u64::from(word.index) | (u64::from(word.generation) << 32);
        (hash.rotate_left(29) ^ value).wrapping_mul(SEED)
    })
}
