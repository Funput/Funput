use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::index::TOP_K;

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
    pub fn len(&self) -> usize {
        self.len
    }

    pub fn is_empty(&self) -> bool {
        self.len == 0
    }

    pub fn iter(&self) -> impl Iterator<Item = &'a str> + '_ {
        self.items[..self.len].iter().filter_map(|item| *item)
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
}
