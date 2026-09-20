use std::array;

use super::SuggestionEngine;
use super::admission::promotion_threshold;
use crate::index::{NONE, TOP_K, normalize};
use crate::types::{SuggestionSet, SuggestionStats, WordRecord};

impl SuggestionEngine {
    pub fn suggest(&self, prefix: &str) -> SuggestionSet<'_> {
        self.suggest_with(None, prefix)
    }

    /// The words the prefix alone would offer, best first. Split out so the
    /// context-aware path can reorder and extend the same set.
    #[inline]
    pub(crate) fn prefix_candidates(&self, prefix: &str) -> ([u32; TOP_K], usize) {
        let scalar_count = normalize::exact_chars(prefix).count();
        if scalar_count == 0 || scalar_count > self.config.max_token_scalars {
            return ([NONE; TOP_K], 0);
        }
        let exact = self.exact.find(normalize::exact_chars(prefix), &self.words);
        let differs = normalize::folded_chars(prefix).ne(normalize::exact_chars(prefix));
        let folded = if differs {
            [NONE; TOP_K]
        } else {
            self.folded
                .find(normalize::folded_chars(prefix), &self.words)
        };

        let mut ids = [NONE; TOP_K];
        let mut len = 0;
        for id in exact.into_iter().chain(folded) {
            if id != NONE && !ids[..len].contains(&id) && len < TOP_K {
                ids[len] = id;
                len += 1;
            }
        }
        (ids, len)
    }

    #[inline]
    pub(crate) fn assemble(&self, ids: [u32; TOP_K], len: usize) -> SuggestionSet<'_> {
        let items = array::from_fn(|index| {
            ids.get(index)
                .copied()
                .filter(|id| *id != NONE)
                .and_then(|id| self.words.get(id as usize))
                .map(|word| word.text.as_str())
        });
        SuggestionSet { items, len }
    }

    /// How many times the user has typed `word`, and 0 for a word the store has
    /// never seen.
    ///
    /// Read-only, no I/O, and no allocation: typo correction asks it once per
    /// candidate at a word boundary, to weigh the words the touch evidence reached
    /// against how likely each one is to be what this user meant.
    pub fn frequency(&self, word: &str) -> u32 {
        self.context_slot(word)
            .and_then(|(index, _)| self.words.get(index as usize))
            .map_or(0, |record| record.uses)
    }

    /// Whether `word` is a word at all — one the user has typed, or one in the
    /// shipped English list.
    ///
    /// This is the veto behind typo correction. `funput-engine` carries no
    /// dictionary, so `text ` reaches it looking exactly like a mistyped Vietnamese
    /// word, and the engine offers what the touch data can reach (`tẻ`, since `r`
    /// sits beside `t`). Asking this first is what tells a deliberate English word
    /// from a slip.
    pub fn is_known_word(&self, word: &str) -> bool {
        self.frequency(word) > 0
            || self
                .lexicon
                .file
                .as_ref()
                .is_some_and(|file| file.contains(word))
    }

    pub fn stats(&self) -> SuggestionStats {
        let word_bytes = self.words.capacity() * size_of::<WordRecord>()
            + self
                .words
                .iter()
                .map(|word| word.text.capacity())
                .sum::<usize>();
        let pending_bytes = self.pending.capacity() * size_of::<crate::persistence::JournalEntry>()
            + self
                .pending
                .iter()
                .map(|(token, _)| token.capacity())
                .sum::<usize>();
        SuggestionStats {
            words: self.words.len(),
            promoted_words: self
                .words
                .iter()
                .filter(|word| word.uses >= promotion_threshold(&word.text, &self.config))
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
