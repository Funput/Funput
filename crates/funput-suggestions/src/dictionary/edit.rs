//! The four changes a person can make to one word.
//!
//! Every edit is idempotent: applying it to a store that already reflects it
//! changes nothing and reports `Unchanged`. That is what makes it safe to replay an
//! edit the journal or the inbox may already have delivered once.

use super::{Edit, EditKind, EditOutcome, Origin};
use crate::engine::SuggestionEngine;
use crate::persistence::JournalEntry;
use crate::types::WordRecord;

impl SuggestionEngine {
    /// Applies one edit. Durable on the next `flush`, like anything learned.
    pub fn apply_edit(&mut self, edit: Edit<'_>) -> EditOutcome {
        self.apply_edits([edit])[0]
    }

    /// Applies edits in order and rebuilds the index once for all of them.
    pub fn apply_edits<'a>(
        &mut self,
        edits: impl IntoIterator<Item = Edit<'a>>,
    ) -> Vec<EditOutcome> {
        let outcomes: Vec<_> = edits
            .into_iter()
            .map(|edit| self.apply_one(edit.kind, edit.word, true))
            .collect();
        if outcomes.contains(&EditOutcome::Applied) {
            self.rebuild_tries();
        }
        outcomes
    }

    /// One edit, journalled when `record` is set — replay sets it off, since the
    /// edit is already in the journal it is being read from. Leaves the tries
    /// stale; the caller rebuilds them once the batch is done.
    pub(crate) fn apply_one(&mut self, kind: EditKind, word: &str, record: bool) -> EditOutcome {
        let Some(key) = self.token_key(word) else {
            return EditOutcome::Invalid;
        };
        let outcome = match kind {
            EditKind::Add => self.add_word(&key),
            EditKind::Forget => self.forget_word(&key),
            EditKind::Block => self.block_word(&key),
            EditKind::Unblock => self.unblock_word(&key),
        };
        if outcome == EditOutcome::Applied {
            // Every change moves the sequence on. A journal frame is only replayed
            // when it ends past the snapshot's sequence, so an edit that left it
            // where it was could be skipped as already folded in.
            self.sequence = self.sequence.saturating_add(1);
            if record {
                self.push_pending(JournalEntry::Edit { kind, word: key });
            }
        }
        // Whatever comes next in the journal no longer follows the last token.
        self.journalled_previous = None;
        outcome
    }

    /// Checks the limit before touching anything, so a refused add leaves even a
    /// block on the word in place.
    fn add_word(&mut self, key: &str) -> EditOutcome {
        let floor = self.config.promotion_uses;
        let slot = self.slot_of(key);
        let already_added = slot.is_some_and(|index| self.words[index].origin == Origin::Added);
        if !already_added && self.added_words() >= self.config.max_added_words {
            return EditOutcome::LimitReached;
        }
        let unblocked = self.exclusions.blocked.remove(key);
        if let Some(index) = slot {
            let word = &mut self.words[index];
            let changed = word.origin != Origin::Added || word.uses < floor;
            word.origin = Origin::Added;
            word.uses = word.uses.max(floor);
            return applied_if(changed || unblocked);
        }
        if self.words.len() >= self.config.max_words
            && let Some(index) = self.lowest_ranked_index()
        {
            self.remove_slot(index);
        }
        // Ranked like a word that has just been promoted, so it is offered at once
        // without outranking what the user actually types.
        let mut record = WordRecord::new(key.to_owned(), floor, self.sequence);
        record.origin = Origin::Added;
        self.words.push(record);
        EditOutcome::Applied
    }

    fn forget_word(&mut self, key: &str) -> EditOutcome {
        match self.slot_of(key) {
            Some(index) => {
                self.remove_slot(index);
                EditOutcome::Applied
            }
            None => EditOutcome::Unchanged,
        }
    }

    /// Checks the limit before forgetting, so a refused block changes nothing.
    fn block_word(&mut self, key: &str) -> EditOutcome {
        let already = self.exclusions.blocked.contains(key);
        if !already && self.exclusions.blocked.len() >= self.config.max_blocked_words {
            return EditOutcome::LimitReached;
        }
        let forgotten = self.forget_word(key) == EditOutcome::Applied;
        let inserted = !already && self.exclusions.blocked.insert(key.to_owned());
        applied_if(forgotten || inserted)
    }

    fn unblock_word(&mut self, key: &str) -> EditOutcome {
        applied_if(self.exclusions.blocked.remove(key))
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
