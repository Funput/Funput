use std::array;

use super::SuggestionEngine;
use crate::index::{NONE, TOP_K, normalize};
use crate::types::{SuggestionSet, SuggestionStats, WordRecord};

impl SuggestionEngine {
    pub fn suggest(&self, prefix: &str) -> SuggestionSet<'_> {
        let scalar_count = normalize::exact_chars(prefix).count();
        if scalar_count == 0 || scalar_count > self.config.max_token_scalars {
            return SuggestionSet {
                items: [None; TOP_K],
                len: 0,
            };
        }
        let exact = self.exact.find(normalize::exact_chars(prefix));
        let differs = normalize::folded_chars(prefix).ne(normalize::exact_chars(prefix));
        let folded = if differs {
            [NONE; TOP_K]
        } else {
            self.folded.find(normalize::folded_chars(prefix))
        };

        let mut ids = [NONE; TOP_K];
        let mut len = 0;
        for id in exact.into_iter().chain(folded) {
            if id != NONE && !ids[..len].contains(&id) && len < TOP_K {
                ids[len] = id;
                len += 1;
            }
        }
        let items = array::from_fn(|index| {
            ids.get(index)
                .copied()
                .filter(|id| *id != NONE)
                .and_then(|id| self.words.get(id as usize))
                .map(|word| word.text.as_str())
        });
        SuggestionSet { items, len }
    }

    pub fn stats(&self) -> SuggestionStats {
        let word_bytes = self.words.capacity() * size_of::<WordRecord>()
            + self
                .words
                .iter()
                .map(|word| word.text.capacity())
                .sum::<usize>();
        let pending_bytes = self.pending.capacity() * size_of::<String>()
            + self.pending.iter().map(String::capacity).sum::<usize>();
        SuggestionStats {
            words: self.words.len(),
            promoted_words: self
                .words
                .iter()
                .filter(|word| word.uses >= self.config.promotion_uses)
                .count(),
            exact_nodes: self.exact.node_count(),
            folded_nodes: self.folded.node_count(),
            pending_mutations: self.pending.len(),
            journal_bytes: self.journal_bytes,
            estimated_heap_bytes: word_bytes
                + pending_bytes
                + self.exact.heap_bytes()
                + self.folded.heap_bytes(),
            last_snapshot_bytes: self.last_snapshot_bytes,
        }
    }
}
