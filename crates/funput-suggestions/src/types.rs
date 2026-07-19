use crate::trie::TOP_K;

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
}
