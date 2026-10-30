//! The two processes around one store.
//!
//! The keyboard owns the store: it opens it with [`SuggestionEngine::open`] and is
//! the only one to write the snapshot or the journal. The app reads it with
//! [`SuggestionEngine::open_snapshot`], posts its edits with [`post_edits`], and the
//! keyboard folds them in with [`SuggestionEngine::apply_pending_edits`] — on open,
//! and whenever the platform calls it.

use std::io;
use std::path::Path;

use super::Edit;
use crate::engine::{SuggestionConfig, SuggestionEngine};
use crate::persistence::inbox;
use crate::persistence::{Access, JournalEntry, Store};

/// Posts edits for the keyboard to apply to the store at `path`.
///
/// For the process that does not own the store. Writes one new file and nothing
/// else, so it is safe while the keyboard has the store open. Words are checked
/// when applied, not here: one the store cannot hold is skipped as `Invalid`.
pub fn post_edits(path: impl AsRef<Path>, edits: &[Edit<'_>]) -> io::Result<()> {
    inbox::post(path.as_ref(), edits)
}

impl SuggestionEngine {
    /// Opens the store at `path` to read, as the app does while the keyboard owns
    /// it. Writes nothing — no directory, no repair, no compact — and includes the
    /// edits still waiting in the inbox, so a listing shows what the store is
    /// about to be.
    ///
    /// The engine has no store behind it: `flush` and `compact` keep nothing, and
    /// further edits change only this view. Post them with [`post_edits`] to keep
    /// them.
    pub fn open_snapshot(path: impl AsRef<Path>, config: SuggestionConfig) -> io::Result<Self> {
        let path = path.as_ref();
        let mut engine = Self::from_loaded(config, Store::read(path)?);
        let pending = inbox::read(path, Access::ReadOnly)?;
        engine.replay(pending.edits);
        Ok(engine)
    }

    /// Applies the edits the app has posted, makes them durable in the journal,
    /// and only then removes them from the inbox. Returns how many were read.
    /// Nothing to do for an engine with no store.
    pub fn apply_pending_edits(&mut self) -> io::Result<usize> {
        let Some(root) = self.store.as_ref().map(|store| store.root().to_owned()) else {
            return Ok(0);
        };
        let pending = inbox::read(&root, Access::Repair)?;
        if pending.files.is_empty() {
            return Ok(0);
        }
        let count = pending.edits.len();
        let mut applied = false;
        for edit in pending.edits {
            if let JournalEntry::Edit { kind, word } = edit {
                applied |= self.apply_one(kind, &word, true) == super::EditOutcome::Applied;
            }
        }
        if applied {
            self.rebuild_tries();
        }
        self.flush()?;
        inbox::remove(&root, &pending.files)?;
        Ok(count)
    }
}
