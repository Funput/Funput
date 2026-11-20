//! Deciding whether the context is worth speaking about on its own.
//!
//! With no prefix, every follower of the context is a candidate, so the question
//! stops being "which of these" and becomes "should anything be offered at all".
//!
//! Vietnamese splits sharply here. "cảm" is nearly always followed by "ơn" and
//! "bởi" by "vì"; "của" and "và" can be followed by anything, and those are the
//! syllables that come up most. No amount of memory helps the second group, so
//! the value of this mode is in staying quiet for it.
//!
//! The bar has three slots, so a context need not have one clear leader to be
//! worth speaking about: "hôm" is followed by "nay" and by "qua", and both
//! belong on the bar. What has to hold is that the words offered account,
//! between them, for most of what follows the context.
//!
//! Staying quiet comes in two kinds, and the difference is what lets a shorter
//! context, and then shipped data, speak without arguing with the user:
//! [`Prediction::Unsure`] is the user's own answer, [`Prediction::NoEvidence`] is
//! the absence of one.

use super::Contexts;
use crate::bigram::follower::{FOLLOWER_SLOTS, Follower, ranked};
use crate::engine::SuggestionEngine;
use crate::index::{NONE, TOP_K};
use crate::ngram::MAX_CONTEXT;
use crate::types::SuggestionSet;

/// The word slots Predict offers, most used first.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) struct Habits {
    ids: [u32; TOP_K],
    len: usize,
}

impl Habits {
    const NONE: Self = Self {
        ids: [NONE; TOP_K],
        len: 0,
    };

    /// Adds `word`, unless it is already offered or every slot is taken.
    fn push(&mut self, word: u32) -> bool {
        let fits = self.len < TOP_K && !self.ids[..self.len].contains(&word);
        if fits {
            self.ids[self.len] = word;
            self.len += 1;
        }
        fits
    }
}

/// What the user's own data says about the word after a context.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Prediction {
    /// The followers the user keeps coming back to account for most of what
    /// follows the context: offer them.
    Speaks(Habits),
    /// The user has typed this context often enough, and what follows it is
    /// spread out. Silence is the answer of the user's own data.
    Unsure,
    /// Too few sightings to say anything, a context the store does not hold, or
    /// a leader that dominates but has not been seen `context_predict_uses`
    /// times yet. A shorter context, or another source, may answer.
    NoEvidence,
}

impl SuggestionEngine {
    /// The bar after a space: the user's own followers, longest context first,
    /// then the shipped pairs in the slots still empty.
    ///
    /// A long context that is `Unsure` ends the question. It has seen enough to
    /// know what follows it is spread out, which is more specific than anything
    /// the word before alone, or a shipped list, could say.
    pub(super) fn predict(
        &self,
        contexts: &Contexts<'_>,
        previous: Option<&str>,
    ) -> SuggestionSet<'_> {
        let mut offered = Habits::NONE;
        for (order, context) in contexts.iter().enumerate() {
            let Some((followers, seen)) = context else {
                continue;
            };
            match self.judge(followers, *seen) {
                Prediction::Speaks(habits) => {
                    for word in &habits.ids[..habits.len] {
                        offered.push(*word);
                    }
                }
                Prediction::Unsure if order + 1 < MAX_CONTEXT => return SuggestionSet::EMPTY,
                Prediction::Unsure | Prediction::NoEvidence => {}
            }
        }
        let mut set = self.assemble(offered.ids, offered.len);
        self.fill_from_pairs(previous, contexts[MAX_CONTEXT - 1], &mut set);
        set
    }

    /// What one context's followers say.
    ///
    /// The followers seen `context_predict_uses` times or more are the user's
    /// habits after this context. They are offered together when they cover
    /// `context_dominance_percent` of its sightings between them. Habits that
    /// fall short of that are a context anything can follow: offering three of
    /// its many followers would be noise, however often each was typed.
    fn judge(&self, followers: &[Follower; FOLLOWER_SLOTS], seen: u16) -> Prediction {
        let mut habits = Habits::NONE;
        let (mut covered, mut leader) = (0, 0);
        for follower in ranked(*followers) {
            if follower.word(&self.words).is_none() {
                continue;
            }
            leader = leader.max(follower.uses);
            if follower.uses >= self.config.context_predict_uses && habits.push(follower.word) {
                covered += u32::from(follower.uses);
            }
        }
        if habits.len > 0 && self.covers(covered, seen) {
            return Prediction::Speaks(habits);
        }
        // A leader too young to trust is too little evidence, not a spread-out
        // answer: `cảm ơn`, `cảm ơn`, `cảm thấy` must not read as a context
        // anything can follow.
        if self.covers(u32::from(leader), seen) || seen < self.config.context_evidence_seen {
            Prediction::NoEvidence
        } else {
            Prediction::Unsure
        }
    }

    /// Both sides are bounded by the aging threshold, so `u32` cannot overflow
    /// and the comparison needs no division.
    fn covers(&self, uses: u32, context_seen: u16) -> bool {
        uses * 100 >= u32::from(context_seen) * u32::from(self.config.context_dominance_percent)
    }
}
