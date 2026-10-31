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
//! Staying quiet comes in two kinds, and the difference is what lets a shorter
//! context, and then shipped data, speak without arguing with the user:
//! [`Prediction::Unsure`] is the user's own answer, [`Prediction::NoEvidence`] is
//! the absence of one.

use super::Contexts;
use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::engine::SuggestionEngine;

/// What the user's own data says about the word after a context.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Prediction {
    /// One follower dominates: offer it.
    Speaks(u32),
    /// The user has typed this context often enough, and what follows it is
    /// spread out. Silence is the answer; nothing else should fill it.
    Unsure,
    /// Too few sightings to say anything, or a context the store does not hold.
    /// A shorter context, or another source, may answer.
    NoEvidence,
}

impl SuggestionEngine {
    /// Longest context first. Only `NoEvidence` steps down: a long context that
    /// is `Unsure` has seen enough to know what follows it is spread out, which is
    /// more specific than anything the word before alone could say.
    pub(super) fn predict(&self, contexts: &Contexts<'_>) -> Prediction {
        for (followers, seen) in contexts.iter().flatten() {
            match self.judge(followers, *seen) {
                Prediction::NoEvidence => continue,
                said => return said,
            }
        }
        Prediction::NoEvidence
    }

    /// What one context's followers say.
    ///
    /// Only the leader is ever offered. The dominance test below is a statement
    /// that the rest of the distribution is noise, so filling the other two slots
    /// with it would contradict the reason this spoke at all.
    fn judge(&self, followers: &[Follower; FOLLOWER_SLOTS], seen: u16) -> Prediction {
        let leader = followers
            .iter()
            .filter(|slot| slot.word(&self.words).is_some())
            .max_by_key(|slot| slot.uses);
        if let Some(leader) = leader
            && leader.uses >= self.config.context_predict_uses
            && self.dominates(leader.uses, seen)
        {
            return Prediction::Speaks(leader.word);
        }
        if seen < self.config.context_evidence_seen {
            Prediction::NoEvidence
        } else {
            Prediction::Unsure
        }
    }

    /// Both sides are bounded by the aging threshold, so `u32` cannot overflow
    /// and the comparison needs no division.
    fn dominates(&self, uses: u16, context_seen: u16) -> bool {
        u32::from(uses) * 100
            >= u32::from(context_seen) * u32::from(self.config.context_dominance_percent)
    }
}
