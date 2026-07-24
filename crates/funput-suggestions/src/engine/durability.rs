use std::io;

use super::{JOURNAL_COMPACT_BYTES, SuggestionEngine};

impl SuggestionEngine {
    pub fn flush(&mut self) -> io::Result<()> {
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
            .saturating_add(store.append_journal(&self.pending)?);
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
        let Some(store) = &self.store else {
            self.pending.clear();
            self.pending_overflow = false;
            self.last_snapshot_bytes = 0;
            self.journal_bytes = 0;
            return Ok(0);
        };
        let bytes = store.compact(&self.words, self.sequence)?;
        self.pending.clear();
        self.pending_overflow = false;
        self.last_snapshot_bytes = bytes;
        self.journal_bytes = 0;
        Ok(bytes)
    }

    pub fn reset(&mut self) -> io::Result<()> {
        self.words.clear();
        self.exact.clear();
        self.folded.clear();
        self.sequence = 0;
        self.pending.clear();
        self.pending_overflow = false;
        self.compact().map(|_| ())
    }
}
