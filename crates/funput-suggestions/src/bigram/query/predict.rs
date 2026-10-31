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
//! Staying quiet comes in two kinds, and the difference is what lets shipped data
//! speak without arguing with the user: [`Prediction::Unsure`] is the user's own
//! answer, [`Prediction::NoEvidence`] is the absence of one.

use crate::engine::SuggestionEngine;

/// What the user's own pairs say about the word after a context.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub(crate) enum Prediction {
    /// One follower dominates: offer it.
    Speaks(u32),
    /// The user has typed this context often enough, and what follows it is
    /// spread out. Silence is the answer; nothing else should fill it.
    Unsure,
    /// Too few sightings to say anything, or a context the store does not hold.
    /// Another source may answer.
    NoEvidence,
}

impl SuggestionEngine {
    /// What the user's pairs say about the word after `context`, the slot of the
    /// previous word if the store holds it.
    ///
    /// Only the leader is ever offered. The dominance test below is a statement
    /// that the rest of the distribution is noise, so filling the other two slots
    /// with it would contradict the reason this spoke at all.
    pub(crate) fn predict(&self, context: Option<u32>) -> Prediction {
        let Some(word) = context.and_then(|index| self.words.get(index as usize)) else {
            return Prediction::NoEvidence;
        };
        let leader = word
            .followers
            .iter()
            .filter(|slot| slot.word(&self.words).is_some())
            .max_by_key(|slot| slot.uses);
        if let Some(leader) = leader
            && leader.uses >= self.config.context_predict_uses
            && self.dominates(leader.uses, word.context_seen)
        {
            return Prediction::Speaks(leader.word);
        }
        if word.context_seen < self.config.context_evidence_seen {
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
