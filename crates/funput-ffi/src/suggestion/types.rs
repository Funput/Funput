use funput_suggestions::{SuggestionSet, SuggestionStats};

use crate::support;

pub const SUGGESTION_CAP: usize = 3;
pub const SUGGESTION_CHARS_CAP: usize = 32;

#[repr(C)]
#[derive(Clone, Copy)]
pub struct FunputSuggestionCandidate {
    pub count: u32,
    pub chars: [u32; SUGGESTION_CHARS_CAP],
}

impl Default for FunputSuggestionCandidate {
    fn default() -> Self {
        Self {
            count: 0,
            chars: [0; SUGGESTION_CHARS_CAP],
        }
    }
}

#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct FunputSuggestionResult {
    pub count: u32,
    pub candidates: [FunputSuggestionCandidate; SUGGESTION_CAP],
}

impl FunputSuggestionResult {
    pub(crate) fn from_set(set: SuggestionSet<'_>) -> Self {
        let mut result = Self::default();
        for (index, text) in set.iter().take(SUGGESTION_CAP).enumerate() {
            let candidate = &mut result.candidates[index];
            candidate.count = support::copy_codepoints(&mut candidate.chars, text.chars()) as u32;
            result.count += 1;
        }
        result
    }
}

#[repr(C)]
#[derive(Clone, Copy, Default)]
pub struct FunputSuggestionStats {
    pub words: u32,
    pub promoted_words: u32,
    pub exact_nodes: u32,
    pub folded_nodes: u32,
    pub pending_mutations: u32,
    pub journal_bytes: u64,
    pub estimated_heap_bytes: u64,
    pub last_snapshot_bytes: u64,
}

impl From<SuggestionStats> for FunputSuggestionStats {
    fn from(stats: SuggestionStats) -> Self {
        Self {
            words: stats.words as u32,
            promoted_words: stats.promoted_words as u32,
            exact_nodes: stats.exact_nodes as u32,
            folded_nodes: stats.folded_nodes as u32,
            pending_mutations: stats.pending_mutations as u32,
            journal_bytes: stats.journal_bytes,
            estimated_heap_bytes: stats.estimated_heap_bytes as u64,
            last_snapshot_bytes: stats.last_snapshot_bytes,
        }
    }
}
