//! Applying edits to the store.
//!
//! - here — the entry points, and the batch every caller goes through: apply each
//!   edit, then rebuild the tries once if anything changed.
//! - `words` — what each of the four edits does to one word.
//! - `removal` — taking one word out without leaving its slot's edges stale.
//!
//! Every edit is idempotent: applying it to a store that already reflects it
//! changes nothing and reports `Unchanged`. That is what makes it safe to replay an
//! edit the journal or the inbox may already have delivered once.

mod removal;
mod words;

use super::{Edit, EditKind, EditOutcome};
use crate::engine::SuggestionEngine;
use crate::persistence::JournalEntry;

/// Where an edit comes from, which decides whether it is checked and kept.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Mode {
    /// Made now, on the store's owner: checked against the limits and journalled.
    Record,
    /// Still waiting in the inbox, shown in the app's view: checked exactly as the
    /// keyboard will check it, and written nowhere.
    Preview,
    /// Read back from the journal: accepted when it was written, so a limit that
    /// has shrunk since must not drop it now.
    Replay,
}

impl Mode {
    fn checks_limits(self) -> bool {
        self != Self::Replay
    }
}

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
        let edits = edits.into_iter().map(|edit| (edit.kind, edit.word));
        self.apply_batch(edits, Mode::Record)
    }

    /// The one way a run of edits reaches the store outside replay: each applied
    /// in order, the tries rebuilt once at the end if any of them changed it.
    pub(crate) fn apply_batch<'a>(
        &mut self,
        edits: impl IntoIterator<Item = (EditKind, &'a str)>,
        mode: Mode,
    ) -> Vec<EditOutcome> {
        let outcomes: Vec<_> = edits
            .into_iter()
            .map(|(kind, word)| self.apply_one(kind, word, mode))
            .collect();
        if outcomes.contains(&EditOutcome::Applied) {
            self.rebuild_tries();
        }
        outcomes
    }

    /// One edit. Leaves the tries stale; the caller rebuilds them once its batch is
    /// done, before anything queries them.
    pub(crate) fn apply_one(&mut self, kind: EditKind, word: &str, mode: Mode) -> EditOutcome {
        let Some(key) = self.token_key(word) else {
            return EditOutcome::Invalid;
        };
        let limits = mode.checks_limits();
        let outcome = match kind {
            EditKind::Add => self.add_word(&key, limits),
            EditKind::Forget => self.forget_word(&key),
            EditKind::Block => self.block_word(&key, limits),
            EditKind::Unblock => self.unblock_word(&key),
        };
        if outcome == EditOutcome::Applied {
            // Every change moves the sequence on. A journal frame is only replayed
            // when it ends past the snapshot's sequence, so an edit that left it
            // where it was could be skipped as already folded in.
            self.sequence = self.sequence.saturating_add(1);
            if mode == Mode::Record {
                self.push_pending(JournalEntry::Edit { kind, word: key });
            }
        }
        // Whatever comes next in the journal no longer follows the last token.
        self.journal_chain.clear();
        outcome
    }
}
