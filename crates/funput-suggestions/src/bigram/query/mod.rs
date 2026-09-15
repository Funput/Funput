//! The read path, in its two modes.
//!
//! - `rerank` — a prefix is being typed, and the context reorders and extends
//!   what that prefix could become.
//! - `predict` — no prefix yet, so the context is all there is to go on.

mod predict;
mod rerank;

use crate::engine::SuggestionEngine;
use crate::types::SuggestionSet;

impl SuggestionEngine {
    /// The words `prefix` could become, with the ones that have followed
    /// `previous` before moved to the front, and any slot still empty filled
    /// from the attached English lexicon.
    ///
    /// `previous` is the caller's business, as it is for `learn_after`: a
    /// platform that cannot vouch for what came before passes `None` and gets
    /// exactly what `suggest` returns.
    #[inline]
    pub fn suggest_with(&self, previous: Option<&str>, prefix: &str) -> SuggestionSet<'_> {
        let (ids, len) = self.prefix_candidates(prefix);
        let (ids, len) = match previous.and_then(|text| self.context_slot(text)) {
            // No prefix means no candidates to reorder, so the context is all
            // there is to go on and a much stricter question applies.
            Some((context, _)) if len == 0 && prefix.is_empty() => self.predict(context),
            Some((context, _)) => self.with_context(context, prefix, ids, len),
            None => (ids, len),
        };
        // An empty prefix finds nothing in the lexicon, so a prediction stays
        // the personal store's alone.
        self.complete_from_lexicon(prefix, self.assemble(ids, len))
    }
}
