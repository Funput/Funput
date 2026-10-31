//! Where the shipped pairs meet the user's own.
//!
//! The user's pairs always speak first: if Predict is sure of something, that is
//! what the bar offers, shipped list or not. The list only fills the silence, and
//! only while the user has typed the first syllable too seldom for their own
//! pairs to be the judge. Past `seed_pairs_until_seen` sightings, a Predict that
//! still says nothing means the user's continuations are spread out or differ
//! from the list, and the list stays quiet rather than argue with them.

use super::SeedLanguage;
use crate::engine::SuggestionEngine;
use crate::types::SuggestionSet;

impl SuggestionEngine {
    /// What the bar offers after `previous` and a space, before anything is typed.
    /// `context` is `previous`'s slot in the store, if it has one.
    pub(crate) fn predict_or_seed(
        &self,
        previous: Option<&str>,
        context: Option<(u32, u16)>,
    ) -> SuggestionSet<'_> {
        if let Some((context, _)) = context {
            let (ids, len) = self.predict(context);
            if len > 0 {
                return self.assemble(ids, len);
            }
            let seen = self
                .words
                .get(context as usize)
                .map_or(0, |word| word.context_seen);
            if seen >= self.config.seed_pairs_until_seen {
                return SuggestionSet::EMPTY;
            }
        }
        let mut set = SuggestionSet::EMPTY;
        if let (SeedLanguage::Vietnamese(style), Some(pairs), Some(previous)) =
            (self.seed.language, self.seed.pairs, previous)
            && let Some(next) = pairs.next_after(previous, style)
            && !self.blocked.contains(next)
        {
            set.items[0] = Some(next);
            set.len = 1;
        }
        set
    }
}
