//! What each of the four edits does to one word.
//!
//! Each checks everything that could refuse it before changing anything, so a
//! refused edit leaves the store exactly as it was.

use crate::dictionary::{EditOutcome, Origin};
use crate::engine::SuggestionEngine;
use crate::types::WordRecord;

impl SuggestionEngine {
    /// `limits` is off only for replay: the edit was accepted when it was written.
    pub(super) fn add_word(&mut self, key: &str, limits: bool) -> EditOutcome {
        let floor = self.config.promotion_uses;
        let slot = self.slot_of(key);
        let already_added = slot.is_some_and(|index| self.words[index].origin == Origin::Added);
        if limits && !already_added && self.added_words() >= self.config.max_added_words {
            return EditOutcome::LimitReached;
        }
        // A full store makes room by evicting a learned word. Holding nothing but
        // added words, it has no room to give: a new add is refused, and a replayed
        // one is kept over capacity, as a snapshot holding it would be on load.
        let evict = match slot {
            None if self.words.len() >= self.config.max_words => match self.lowest_ranked_index() {
                Some(index) => Some(index),
                None if limits => return EditOutcome::LimitReached,
                None => None,
            },
            _ => None,
        };
        let unblocked = self.blocked.remove(key);
        if let Some(index) = slot {
            let word = &mut self.words[index];
            let changed = word.origin != Origin::Added || word.uses < floor;
            word.origin = Origin::Added;
            word.uses = word.uses.max(floor);
            return applied_if(changed || unblocked);
        }
        if let Some(index) = evict {
            self.remove_slot(index);
        }
        // Ranked like a word that has just been promoted, so it is offered at once
        // without outranking what the user actually types.
        let mut record = WordRecord::new(key.to_owned(), floor, self.sequence);
        record.origin = Origin::Added;
        self.words.push(record);
        EditOutcome::Applied
    }

    pub(super) fn forget_word(&mut self, key: &str) -> EditOutcome {
        match self.slot_of(key) {
            Some(index) => {
                self.remove_slot(index);
                EditOutcome::Applied
            }
            None => EditOutcome::Unchanged,
        }
    }

    pub(super) fn block_word(&mut self, key: &str, limits: bool) -> EditOutcome {
        let already = self.blocked.contains(key);
        if limits && !already && self.blocked.len() >= self.config.max_blocked_words {
            return EditOutcome::LimitReached;
        }
        let forgotten = self.forget_word(key) == EditOutcome::Applied;
        let inserted = !already && self.blocked.insert(key.to_owned());
        applied_if(forgotten || inserted)
    }

    pub(super) fn unblock_word(&mut self, key: &str) -> EditOutcome {
        applied_if(self.blocked.remove(key))
    }

    fn added_words(&self) -> usize {
        self.words
            .iter()
            .filter(|word| word.origin == Origin::Added)
            .count()
    }
}

fn applied_if(changed: bool) -> EditOutcome {
    if changed {
        EditOutcome::Applied
    } else {
        EditOutcome::Unchanged
    }
}
