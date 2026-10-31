//! The read path, in its two modes.
//!
//! - `rerank` — a prefix is being typed, and the context reorders and extends
//!   what that prefix could become.
//! - `predict` — no prefix yet, so the context is all there is to go on.
//!
//! Both read the longest context the store has first: the two words before from
//! the trigram table, then the word before from its own followers.

mod predict;
mod rerank;

use predict::Prediction;

use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::engine::SuggestionEngine;
use crate::index::{NONE, TOP_K};
use crate::ngram::{MAX_CONTEXT, WordRef};
use crate::types::SuggestionSet;

/// The follower sets a context offers, longest context first.
type Contexts<'a> = [Option<(&'a [Follower; FOLLOWER_SLOTS], u16)>; MAX_CONTEXT];

impl SuggestionEngine {
    /// [`Self::suggest_in`] with one word of context, or none.
    #[inline]
    pub fn suggest_with(&self, previous: Option<&str>, prefix: &str) -> SuggestionSet<'_> {
        self.suggest_in(previous.as_slice(), prefix)
    }

    /// The words `prefix` could become, with the ones that have followed the
    /// `context` before moved to the front, and any slot still empty filled from
    /// the shipped Vietnamese data and the attached English lexicon. With no
    /// prefix, the word most likely to follow the context, from the user's own
    /// pairs and trigrams or the shipped pairs.
    ///
    /// `context` is the words before, oldest first; only the last two are read.
    /// It is the caller's business, as it is for `learn_after`: a platform that
    /// cannot vouch for what came before passes nothing and gets exactly what
    /// `suggest` returns.
    pub fn suggest_in(&self, context: &[&str], prefix: &str) -> SuggestionSet<'_> {
        let contexts = self.contexts(context);
        let previous = context.last().copied();
        if prefix.is_empty() {
            // No prefix means no candidates to reorder, so the context is all
            // there is to go on and a much stricter question applies. The user's
            // own data answers first, longest context first; shipped data only
            // speaks where none of it has evidence either way. The English
            // lexicon has nothing to add: an empty prefix finds nothing in it.
            return match self.predict(&contexts) {
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
        let (ids, len) = self.with_context(&contexts, prefix, ids, len);
        let mut set = self.assemble(ids, len);
        let yields = self.yields_to(&set);
        let mut seeds = self.seed_words(previous, prefix, &set);
        seeds.fill_before_english(&mut set);
        let mut set = self.complete_from_lexicon(prefix, set, yields);
        seeds.fill_rest(&mut set);
        set
    }

    /// What the store holds about `context`: the trigram entry for its last two
    /// words when the order allows and the table has one, then the last word's
    /// own followers.
    fn contexts(&self, context: &[&str]) -> Contexts<'_> {
        let [older, previous] = self.context_slots(context);
        let trigram = match (older, previous) {
            (Some(older), Some(previous)) if self.config.context_order >= 3 => self
                .trigrams
                .find(&[older, previous])
                .map(|entry| (&entry.followers, entry.context_seen)),
            _ => None,
        };
        let bigram = previous
            .and_then(|WordRef { index, .. }| self.words.get(index as usize))
            .map(|word| (&word.followers, word.context_seen));
        [trigram, bigram]
    }
}
