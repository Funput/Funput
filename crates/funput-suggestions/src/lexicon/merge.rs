//! Where the English lexicon meets the personal store: the call that gives the
//! bar its three words.
//!
//! The personal candidates always come first. A word the user typed is the
//! strongest evidence there is, and a personal `uses` count and a dictionary
//! rank measure different things, so no exchange rate between them is invented:
//! the lexicon only fills the slots left empty. Its own top three always
//! suffice — the `3 − |P|` empty slots never outnumber the lexicon words that
//! are not already in P.
//!
//! One exception: once the store is plainly Vietnamese, a personal answer that
//! holds a marked word is Vietnamese typing, and English in its empty slots
//! would be noise — `an` should offer `anh · ăn`, not `and`. The lexicon then
//! stays out. Unmarked answers (`hai`, `work`) still get filled.

use std::io;
use std::path::Path;

use super::Lexicon;
use crate::engine::SuggestionEngine;
use crate::index::{TOP_K, normalize};
use crate::types::SuggestionSet;

/// What an engine holds of the English lexicon.
#[derive(Default)]
pub(crate) struct LexiconSlot {
    pub(crate) file: Option<Lexicon>,
    /// Promoted words carrying Vietnamese marks. Kept by the learn path: up on
    /// promotion, down on eviction, recounted by every trie rebuild, zero on
    /// reset. Never scanned for on the query path.
    pub(crate) vietnamese_words: u32,
}

impl SuggestionEngine {
    /// Attaches the `en.lex` at `path`, replacing any lexicon already attached.
    ///
    /// On error nothing changes: the engine keeps the lexicon it had, or none,
    /// and suggests exactly as before. `reset` leaves the lexicon attached —
    /// it is shipped data, not the user's.
    pub fn attach_lexicon(&mut self, path: impl AsRef<Path>) -> io::Result<()> {
        self.lexicon.file = Some(Lexicon::open(path.as_ref())?);
        Ok(())
    }

    /// `personal`, with any slot it left empty filled from the lexicon.
    pub(crate) fn complete_from_lexicon<'a>(
        &'a self,
        prefix: &str,
        personal: SuggestionSet<'a>,
    ) -> SuggestionSet<'a> {
        let Some(file) = &self.lexicon.file else {
            return personal;
        };
        if personal.len >= TOP_K || self.yields_to(&personal) {
            return personal;
        }
        let mut merged = personal;
        for word in file.top3(prefix).iter() {
            // A learned word is stored lowercase and the lexicon keeps its own
            // case, so `iphone` and `iPhone` are the same suggestion.
            if personal.iter().any(|mine| mine.eq_ignore_ascii_case(word)) {
                continue;
            }
            let Some(slot) = merged.items.get_mut(merged.len) else {
                break;
            };
            *slot = Some(word);
            merged.len += 1;
        }
        merged
    }

    /// Whether `personal` is Vietnamese typing in a Vietnamese store.
    fn yields_to(&self, personal: &SuggestionSet<'_>) -> bool {
        self.lexicon.vietnamese_words >= self.config.lexicon_yield_after_words
            && personal.iter().any(normalize::is_marked)
    }
}
