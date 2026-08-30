//! Deciding whether the context is worth speaking about on its own.
//!
//! With no prefix, every follower of the context is a candidate, so the question
//! stops being "which of these" and becomes "should anything be offered at all".
//!
//! Vietnamese splits sharply here. "cảm" is nearly always followed by "ơn" and
//! "bởi" by "vì"; "của" and "và" can be followed by anything, and those are the
//! syllables that come up most. No amount of memory helps the second group, so
//! the value of this mode is in staying quiet for it.

use crate::engine::SuggestionEngine;
use crate::index::{NONE, TOP_K};

impl SuggestionEngine {
    /// The one word most likely to follow `context`, or nothing.
    ///
    /// Only the leader is ever offered. The dominance test below is a statement
    /// that the rest of the distribution is noise, so filling the other two slots
    /// with it would contradict the reason this spoke at all.
    pub(super) fn predict(&self, context: u32) -> ([u32; TOP_K], usize) {
        let silent = ([NONE; TOP_K], 0);
        let Some(word) = self.words.get(context as usize) else {
            return silent;
        };
        let Some(leader) = word
            .followers
            .iter()
            .filter(|slot| slot.word(&self.words).is_some())
            .max_by_key(|slot| slot.uses)
        else {
            return silent;
        };
        if leader.uses < self.config.context_predict_uses
            || !self.dominates(leader.uses, word.context_seen)
        {
            return silent;
        }
        let mut ids = [NONE; TOP_K];
        ids[0] = leader.word;
        (ids, 1)
    }

    /// Both sides are bounded by the aging threshold, so `u32` cannot overflow
    /// and the comparison needs no division.
    fn dominates(&self, uses: u16, context_seen: u16) -> bool {
        u32::from(uses) * 100
            >= u32::from(context_seen) * u32::from(self.config.context_dominance_percent)
    }
}
