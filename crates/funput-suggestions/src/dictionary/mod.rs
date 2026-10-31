//! The personal dictionary: the learned store seen as a list the user can read and
//! change one word at a time.
//!
//! - `listing` — what the store holds, filtered, searched and sorted for display.
//! - `edit/` — the four changes a person can make (add, forget, block, unblock),
//!   applied in batches, and taking one word out without leaving edges stale.
//! - `blocked` — the words the user blocked, which the bar and the English
//!   lexicon never offer.
//! - `view` — the app's read-only open, and the keyboard draining the edits the
//!   app posted while the store was the keyboard's to write.
//!
//! Edits reach the store the same way learning does — journalled, replayed on open,
//! folded into the snapshot by a compact — so a crash loses at most what was not
//! yet flushed, and every edit is idempotent, so replaying one twice is harmless.
//!
//! See `docs/features/personal-dictionary.md`.

mod blocked;
mod edit;
mod listing;
mod view;

pub(crate) use blocked::BlockedWords;
pub(crate) use edit::Mode;

pub use listing::{DictionaryEntry, DictionaryFilter, DictionaryQuery, DictionarySort};
pub use view::post_edits;

/// How a word came to be in the store.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Default)]
pub enum Origin {
    /// Typed often enough to be learned.
    #[default]
    Learned,
    /// Added by hand. Offered at once and never evicted to make room.
    Added,
}

/// What a person can do to one word.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum EditKind {
    /// Add the word, or keep one already learned. Lifts a block on it.
    Add,
    /// Drop the word and what it has learned. Typing it again relearns it.
    Forget,
    /// Drop the word and never learn or offer it again, English lexicon included.
    Block,
    /// Lift a block.
    Unblock,
}

/// One change to the dictionary.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub struct Edit<'a> {
    pub kind: EditKind,
    pub word: &'a str,
}

impl<'a> Edit<'a> {
    pub fn new(kind: EditKind, word: &'a str) -> Self {
        Self { kind, word }
    }
}

/// What an edit did.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum EditOutcome {
    Applied,
    /// The store already looked like this; nothing was written.
    Unchanged,
    /// Not a word the store could hold: empty, spaced, or longer than any token.
    Invalid,
    /// The added-word or blocked-word list is full.
    LimitReached,
}
