use std::array;

use super::{MAX_TOKEN_SCALARS, SuggestionEngine};
use crate::index::{NONE, TOP_K, marks, normalize};
use crate::types::{SuggestionSet, SuggestionStats, WordRecord};

impl SuggestionEngine {
    pub fn suggest(&self, prefix: &str) -> SuggestionSet<'_> {
        self.suggest_with(None, prefix)
    }

    /// The words the prefix alone would offer, best first. Split out so the
    /// context-aware path can reorder and extend the same set.
    ///
    /// Exact prefix matches first. Then, for an unmarked prefix, the folded trie, as
    /// always; for a marked one, every word the marks typed still allow
    /// (`marks::matches`, letter by letter through the exact trie): `đươ` reaches a
    /// learned `đường` before its tone is typed, and the walk finds the true best
    /// three of all such words, not a filtered remnant of some node's three.
    #[inline]
    pub(crate) fn prefix_candidates(&self, prefix: &str) -> ([u32; TOP_K], usize) {
        let none = ([NONE; TOP_K], 0);
        // Normalized once: the count, both lookups and the filter all read `typed`.
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let Some(typed) = normalize::exact_into(prefix, &mut buffer) else {
            return none;
        };
        let scalar_count = typed.chars().count();
        if scalar_count == 0 || scalar_count > self.config.max_token_scalars {
            return none;
        }
        let exact = self.exact.find(typed.chars(), &self.words);
        let folded = if normalize::folded_chars(typed).eq(typed.chars()) {
            self.folded.find(typed.chars(), &self.words)
        } else {
            self.exact.find_matching(typed, &self.words, marks::matches)
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
            .and_then(|slot| self.words.get(slot.index as usize))
            .map_or(0, |record| record.uses)
    }

    /// Whether `word` is a word at all — one the user has typed, added or blocked,
    /// one in the shipped English list, or one of the words typed on purpose that
    /// correction must keep (`ko`, `dc`, a brand name; see
    /// `data/correction/keep.txt`). A blocked word is still a word: the user typed
    /// it on purpose, and correction must not turn it into another.
    ///
    /// This is the veto behind typo correction. `funput-engine` carries no
    /// dictionary, so `text ` reaches it looking exactly like a mistyped Vietnamese
    /// word, and the engine offers what the touch data can reach (`tẻ`, since `r`
    /// sits beside `t`). Asking this first is what tells a deliberate word from a
    /// slip.
    pub fn is_known_word(&self, word: &str) -> bool {
        crate::keep::is_kept(word)
            || self.context_slot(word).is_some()
            || self.blocked.contains(word)
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
                .map(crate::persistence::JournalEntry::heap_bytes)
                .sum::<usize>();
        SuggestionStats {
            words: self.words.len(),
            promoted_words: self.words.iter().filter(|word| self.offers(word)).count(),
            exact_nodes: self.exact.node_count(),
            folded_nodes: self.folded.node_count(),
            pending_mutations: self.pending.len(),
            journal_bytes: self.journal_bytes,
            estimated_heap_bytes: word_bytes
                + pending_bytes
                + self.blocked.heap_bytes()
                + self.trigrams.heap_bytes()
                + self.exact.heap_bytes()
                + self.folded.heap_bytes(),
            last_snapshot_bytes: self.last_snapshot_bytes,
        }
    }
}
