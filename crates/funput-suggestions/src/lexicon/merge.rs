//! Where the English lexicon meets the personal store: the call that gives the
//! bar its three words.
//!
//! The personal candidates always come first. A word the user typed is the
//! strongest evidence there is, and a personal `uses` count and a dictionary
//! rank measure different things, so no exchange rate between them is invented:
//! the lexicon only fills the slots left empty. Its own top three always
//! suffice — the `3 − |P|` empty slots never outnumber the lexicon words that
//! are not already in P.

use std::io;
use std::path::Path;

use super::Lexicon;
use crate::engine::SuggestionEngine;
use crate::index::TOP_K;
use crate::types::SuggestionSet;

/// What an engine holds of the English lexicon.
#[derive(Default)]
pub(crate) struct LexiconSlot {
    pub(crate) file: Option<Lexicon>,
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
        if personal.len >= TOP_K {
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
}
