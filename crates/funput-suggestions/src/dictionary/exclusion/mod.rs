//! What the bar never offers, whatever the store or the lexicon would say.
//!
//! Today that is the words the user blocked (`blocked`). They are persisted with
//! the store and never learned: a blocked word has no record, so the tries cannot
//! hold it, and only the English lexicon still needs asking. A later rule about
//! what never to offer belongs here, beside it, so the query path keeps asking
//! one place.

mod blocked;

pub(crate) use blocked::BlockedWords;

#[derive(Debug, Default)]
pub(crate) struct Exclusions {
    pub(crate) blocked: BlockedWords,
}

impl Exclusions {
    /// Whether a word with no record of its own — an English lexicon word — is
    /// kept out of the bar.
    pub(crate) fn hides_text(&self, word: &str) -> bool {
        self.blocked.contains(word)
    }
}
