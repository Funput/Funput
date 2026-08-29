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
    /// `previous` before moved to the front.
    ///
    /// `previous` is the caller's business, as it is for `learn_after`: a
    /// platform that cannot vouch for what came before passes `None` and gets
    /// exactly what `suggest` has always returned.
    #[inline]
    pub fn suggest_with(&self, previous: Option<&str>, prefix: &str) -> SuggestionSet<'_> {
        let (ids, len) = self.prefix_candidates(prefix);
        let Some((context, _)) = previous.and_then(|text| self.context_slot(text)) else {
            return self.assemble(ids, len);
        };
        let (ids, len) = self.with_context(context, prefix, ids, len);
        self.assemble(ids, len)
    }
}
