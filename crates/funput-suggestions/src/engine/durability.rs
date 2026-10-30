use std::io;

use super::{JOURNAL_COMPACT_BYTES, SuggestionEngine};
use crate::persistence::JournalEntry;

impl SuggestionEngine {
    pub fn flush(&mut self) -> io::Result<()> {
        // Idle is where the sweep belongs, and this is the crate's only idle
        // signal: both shells call it off the typing path. Before the early
        // return, so an in-memory engine with nothing pending is swept too.
        if self.evictions_since_rebuild > 0 {
            self.rebuild_tries();
        }
        if self.pending.is_empty() && !self.pending_overflow {
            return Ok(());
        }
        if self.pending_overflow {
            return self.compact().map(|_| ());
        }
        let Some(store) = &self.store else {
            self.pending.clear();
            return Ok(());
        };
        self.journal_bytes = self
            .journal_bytes
            .saturating_add(store.append_journal(&self.pending, self.sequence)?);
        self.pending.clear();
        if self.journal_bytes >= JOURNAL_COMPACT_BYTES
            && let Err(error) = self.compact()
        {
            self.pending_overflow = true;
            return Err(error);
        }
        Ok(())
    }

    pub fn compact(&mut self) -> io::Result<u64> {
        // The journal is replaced wholesale below, so nothing already written is
        // adjacent to whatever is learned next.
        self.journalled_previous = None;
        let Some(store) = &self.store else {
            self.pending.clear();
            self.pending_overflow = false;
            self.last_snapshot_bytes = 0;
            self.journal_bytes = 0;
            return Ok(0);
        };
        let bytes = store.compact(
            &self.words,
            self.exclusions.blocked.as_slice(),
            self.sequence,
        )?;
        self.pending.clear();
        self.pending_overflow = false;
        self.last_snapshot_bytes = bytes;
        self.journal_bytes = 0;
        Ok(bytes)
    }

    /// Forgets every word, learned or added. The blocked list stays: it is a choice
    /// the user made about what never to see, not something the store learned.
    pub fn reset(&mut self) -> io::Result<()> {
        self.words.clear();
        self.evictions_since_rebuild = 0;
        self.journalled_previous = None;
        self.exact.clear();
        self.folded.clear();
        self.lexicon.vietnamese_words = 0;
        self.sequence = 0;
        self.pending.clear();
        self.pending_overflow = false;
        self.compact().map(|_| ())
    }

    /// Replays journal entries read on open, without writing them again.
    pub(crate) fn replay(&mut self, journal: Vec<JournalEntry>) {
        let mut previous: Option<String> = None;
        let mut edited = false;
        for entry in journal {
            match entry {
                JournalEntry::Learned { token, chained } => {
                    let context = chained.then_some(previous.as_deref()).flatten();
                    self.learn_after_inner(context, &token, false);
                    previous = Some(token);
                }
                JournalEntry::Edit { kind, word } => {
                    self.apply_one(kind, &word, false);
                    edited = true;
                    previous = None;
                }
            }
        }
        if edited {
            self.rebuild_tries();
        }
        self.pending.clear();
        self.pending_overflow = false;
    }
}
