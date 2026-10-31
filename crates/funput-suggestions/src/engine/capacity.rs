//! Which word makes room when the store is full, and trimming a store loaded
//! under a smaller `max_words`.

use super::SuggestionEngine;
use crate::dictionary::Origin;

impl SuggestionEngine {
    /// The learned word that should make room next. Words added by hand never do.
    pub(crate) fn lowest_ranked_index(&self) -> Option<usize> {
        self.words
            .iter()
            .enumerate()
            .filter(|(_, word)| word.origin == Origin::Learned)
            .min_by(|(_, left), (_, right)| {
                left.uses
                    .cmp(&right.uses)
                    .then_with(|| left.last_used.cmp(&right.last_used))
                    .then_with(|| right.text.cmp(&left.text))
            })
            .map(|(index, _)| index)
    }

    /// Only ever called while loading, which rebuilds the tries straight after:
    /// `remove_slot` leaves them stale.
    ///
    /// The trigram table is dropped rather than relocated once per removed word:
    /// a store only shrinks when a build lowers `max_words`, and walking the table
    /// for each of a thousand removals would turn that one load into seconds. The
    /// trigrams are relearned from typing; the words and pairs are kept.
    pub(super) fn enforce_capacity(&mut self) {
        if self.words.len() > self.config.max_words {
            self.trigrams.clear();
        }
        while self.words.len() > self.config.max_words {
            let Some(index) = self.lowest_ranked_index() else {
                break;
            };
            self.remove_slot(index);
        }
    }
}
