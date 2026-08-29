//! The edge a word keeps to a word that followed it.

use crate::index::NONE;
use crate::types::WordRecord;

/// How many following words one word remembers.
///
/// Not a memory compromise — four slots for every word costs about 160 KB in
/// total. It is where the data stops being dense enough to be worth a slot: with
/// a lexicon learned from one person, further slots need evidence that never
/// arrives and fill with typos instead.
pub(crate) const FOLLOWER_SLOTS: usize = 4;

#[derive(Debug, Clone, Copy)]
pub(crate) struct Follower {
    pub(crate) word: u32,
    pub(crate) generation: u16,
    pub(crate) uses: u16,
}

impl Follower {
    pub(crate) const EMPTY: Self = Self {
        word: NONE,
        generation: 0,
        uses: 0,
    };

    pub(crate) fn of(word: u32, words: &[WordRecord]) -> Option<Self> {
        words.get(word as usize).map(|record| Self {
            word,
            generation: record.generation,
            uses: 1,
        })
    }

    /// The word this edge still points at, if it points at one at all.
    ///
    /// The same trap and the same answer as `index::trie::Entry::word`: the
    /// engine hands word slots to new words, so an edge only means anything
    /// while the generation it was written at still matches.
    pub(crate) fn word<'a>(&self, words: &'a [WordRecord]) -> Option<&'a WordRecord> {
        if self.is_free() {
            return None;
        }
        words
            .get(self.word as usize)
            .filter(|record| record.generation == self.generation)
    }

    pub(crate) fn is_free(&self) -> bool {
        self.uses == 0
    }

    pub(crate) fn points_at(&self, other: Self) -> bool {
        !self.is_free() && self.word == other.word && self.generation == other.generation
    }
}
