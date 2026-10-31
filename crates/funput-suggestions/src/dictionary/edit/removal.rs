//! Taking one word out of the store.
//!
//! Words live in a dense `Vec` and everything else refers to them by index: the
//! tries, and every other word's follower edges. `swap_remove` keeps the list dense
//! by moving the last word into the hole, so removal is a matter of telling the
//! edges where that word went. The tries are left stale on purpose — the caller
//! rebuilds them once for a whole batch, which is all a rebuild costs either way.

use crate::bigram::follower::Follower;
use crate::bigram::slots;
use crate::engine::SuggestionEngine;

impl SuggestionEngine {
    /// Removes the word at `index`. The tries are stale until the next
    /// `rebuild_tries`, which every caller runs before anything queries them.
    pub(crate) fn remove_slot(&mut self, index: usize) {
        let Some(removed) = self.words.get(index) else {
            return;
        };
        let removed = key(index, removed.generation);
        let vacated = self.words.len() - 1;
        let moved_from = self
            .words
            .get(vacated)
            .map(|word| key(vacated, word.generation));
        self.words.swap_remove(index);

        // The moved word takes a generation above anything either slot has held,
        // so no edge written against an earlier tenant of `index` can answer to it.
        let moved = match (moved_from, self.words.get_mut(index)) {
            (Some(from), Some(word)) => {
                word.generation = from.generation.max(removed.generation).wrapping_add(1);
                Some((from, key(index, word.generation)))
            }
            _ => None,
        };
        let vacated = vacated as u32;
        for word in &mut self.words {
            slots::relocate(
                &mut word.followers,
                &mut word.context_seen,
                removed,
                vacated,
                moved,
            );
        }
        // Adjacency in the journal is by slot, and the slots just changed.
        self.journalled_previous = None;
    }

    /// The slot holding `word`, if the store has it.
    pub(crate) fn slot_of(&self, exact: &str) -> Option<usize> {
        self.words.iter().position(|word| word.text == exact)
    }
}

/// A slot and generation, in the shape edges are compared in.
fn key(index: usize, generation: u16) -> Follower {
    Follower {
        word: index as u32,
        generation,
        uses: 1,
    }
}
