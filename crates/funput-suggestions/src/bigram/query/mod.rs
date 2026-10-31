//! The read path, in its two modes.
//!
//! - `rerank` — a prefix is being typed, and the context reorders and extends
//!   what that prefix could become.
//! - `predict` — no prefix yet, so the context is all there is to go on.

mod predict;
mod rerank;

use predict::Prediction;

use crate::engine::SuggestionEngine;
use crate::index::{NONE, TOP_K};
use crate::types::SuggestionSet;

impl SuggestionEngine {
    /// The words `prefix` could become, with the ones that have followed
    /// `previous` before moved to the front, and any slot still empty filled from
    /// the shipped Vietnamese data and the attached English lexicon. With no
    /// prefix, the word most likely to follow `previous`, from the user's own
    /// pairs or the shipped ones.
    ///
    /// `previous` is the caller's business, as it is for `learn_after`: a
    /// platform that cannot vouch for what came before passes `None` and gets
    /// exactly what `suggest` returns.
    #[inline]
    pub fn suggest_with(&self, previous: Option<&str>, prefix: &str) -> SuggestionSet<'_> {
        let context = previous.and_then(|text| self.context_slot(text));
        if prefix.is_empty() {
            // No prefix means no candidates to reorder, so the context is all
            // there is to go on and a much stricter question applies. The user's
            // pairs answer first; shipped data only speaks where they have no
            // evidence either way. The English lexicon has nothing to add: an
            // empty prefix finds nothing in it.
            return match self.predict(context.map(|(slot, _)| slot)) {
                Prediction::Speaks(word) => {
                    let mut ids = [NONE; TOP_K];
                    ids[0] = word;
                    self.assemble(ids, 1)
                }
                Prediction::Unsure => SuggestionSet::EMPTY,
                Prediction::NoEvidence => self.seed_after(previous),
            };
        }
        let (ids, len) = self.prefix_candidates(prefix);
        let (ids, len) = match context {
            Some((context, _)) => self.with_context(context, prefix, ids, len),
            None => (ids, len),
        };
        let mut set = self.assemble(ids, len);
        let yields = self.yields_to(&set);
        let mut seeds = self.seed_words(previous, prefix, &set);
        seeds.fill_before_english(&mut set);
        let mut set = self.complete_from_lexicon(prefix, set, yields);
        seeds.fill_rest(&mut set);
        set
    }
}
