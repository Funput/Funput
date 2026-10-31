//! What the shipped data offers on its own, once the user's pairs have no
//! evidence either way.
//!
//! The decision to ask is not made here: `suggest_with` asks only when Predict
//! reports `Prediction::NoEvidence`, so the shipped list never argues with a
//! user whose own continuations are spread out.

use super::SeedSlot;
use crate::engine::SuggestionEngine;
use crate::types::SuggestionSet;

impl SuggestionEngine {
    /// The shipped word after `previous`, or nothing: data off, no pair listed,
    /// or a word the user blocked in either tone style.
    pub(crate) fn seed_after(&self, previous: Option<&str>) -> SuggestionSet<'_> {
        let mut set = SuggestionSet::EMPTY;
        if let SeedSlot::Vietnamese { style, pairs } = self.seed
            && let Some(next) = previous.and_then(|text| pairs.next_after(text))
            && !next
                .spellings()
                .any(|spelling| self.blocked.contains(spelling))
        {
            set.items[0] = Some(next.spelled(style));
            set.len = 1;
        }
        set
    }
}
