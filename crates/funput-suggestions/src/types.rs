use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::dictionary::Origin;
use crate::index::{NONE, TOP_K};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum LearnOutcome {
    Ignored,
    Recorded,
    Promoted,
    Updated,
}

#[derive(Debug, Clone, Copy, Default, PartialEq, Eq)]
pub struct SuggestionStats {
    pub words: usize,
    pub promoted_words: usize,
    pub exact_nodes: usize,
    pub folded_nodes: usize,
    pub pending_mutations: usize,
    pub journal_bytes: u64,
    pub estimated_heap_bytes: usize,
    pub last_snapshot_bytes: u64,
}

#[derive(Debug, Clone, Copy)]
pub struct SuggestionSet<'a> {
    pub(crate) items: [Option<&'a str>; TOP_K],
    pub(crate) len: usize,
}

impl<'a> SuggestionSet<'a> {
    pub(crate) const EMPTY: Self = Self {
        items: [None; TOP_K],
        len: 0,
    };

    pub fn len(&self) -> usize {
        self.len
    }

    pub fn is_empty(&self) -> bool {
        self.len == 0
    }

    pub fn iter(&self) -> impl Iterator<Item = &'a str> + '_ {
        self.items[..self.len].iter().filter_map(|item| *item)
    }

    /// Adds `word` to the first empty slot. False when there is none.
    pub(crate) fn push(&mut self, word: &'a str) -> bool {
        let Some(slot) = self.items.get_mut(self.len) else {
            return false;
        };
        *slot = Some(word);
        self.len += 1;
        true
    }
}

#[derive(Debug, Clone)]
pub(crate) struct WordRecord {
    pub(crate) text: String,
    pub(crate) uses: u32,
    pub(crate) last_used: u64,
    /// Bumped every time this slot is handed to a different word. Anything that
    /// cached the slot's index carries the generation it saw, so a reused slot
    /// invalidates those references instead of silently renaming what they point
    /// at. In memory only — a reloaded store starts everyone back at zero.
    pub(crate) generation: u16,
    /// How many times this word has been seen *as a context* — the denominator
    /// the dominance threshold will divide by once anything reads these edges.
    pub(crate) context_seen: u16,
    /// The words seen following this one. Inline rather than in a table of their
    /// own: the edges then cost no allocation, and a word losing its slot takes
    /// its edges with it instead of leaving a table to go stale.
    pub(crate) followers: [Follower; FOLLOWER_SLOTS],
    /// Learned or added by hand. Written with the record.
    pub(crate) origin: Origin,
}

impl WordRecord {
    /// A record with no history: no edges, generation zero, learned.
    pub(crate) fn new(text: String, uses: u32, last_used: u64) -> Self {
        Self {
            text,
            uses,
            last_used,
            generation: 0,
            context_seen: 0,
            followers: [Follower::EMPTY; FOLLOWER_SLOTS],
            origin: Origin::Learned,
        }
    }
}

/// A word slot and the generation it was written at: what an edge, a trigram key
/// and a removal all name a word by.
///
/// The engine hands word slots to new words, so a reference only means anything
/// while the generation it was taken at still matches.
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
}
