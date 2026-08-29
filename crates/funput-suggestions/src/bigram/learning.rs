//! The write path: learn a token, and remember what it followed.

use super::follower::{FOLLOWER_SLOTS, Follower};
use super::slots;
use crate::engine::SuggestionEngine;
use crate::index::normalize;
use crate::types::LearnOutcome;

impl SuggestionEngine {
    /// Learn `token`, and record that it followed `previous`.
    ///
    /// What counts as `previous` is the caller's business. This crate never looks
    /// at document context, so a platform that cannot vouch for what came before
    /// — a fresh focus, a moved caret, a sentence boundary — passes `None` and
    /// gets exactly the behaviour `learn` always had.
    pub fn learn_after(&mut self, previous: Option<&str>, token: &str) -> LearnOutcome {
        // Resolved before learning because learning `token` can evict `previous`,
        // and re-checked after because it may have just done so.
        let context = previous.and_then(|text| self.context_slot(text));
        let (outcome, word_id) = self.learn_inner(token, true);
        if outcome != LearnOutcome::Ignored
            && let Some((index, generation)) = context
            && self.words[index].generation == generation
        {
            self.record_edge(index, word_id);
        }
        outcome
    }

    /// The slot `previous` sits in, and the generation it sits there at.
    ///
    /// Compares through the normalizing iterator rather than `normalize::exact`,
    /// which would allocate a `String` for every token learned with a context.
    fn context_slot(&self, previous: &str) -> Option<(usize, u16)> {
        self.words
            .iter()
            .position(|word| word.text.chars().eq(normalize::exact_chars(previous)))
            .map(|index| (index, self.words[index].generation))
    }

    fn record_edge(&mut self, context: usize, next_id: u32) {
        let Some(next) = Follower::of(next_id, &self.words) else {
            return;
        };
        // Liveness is read before the mutable borrow: `record` holds one slot of
        // `words` exclusively and so cannot ask `words` anything while it works.
        let followers: [Follower; FOLLOWER_SLOTS] = self.words[context].followers;
        let dead = followers.map(|slot| !slot.is_free() && slot.word(&self.words).is_none());

        let word = &mut self.words[context];
        slots::record(&mut word.followers, &mut word.context_seen, dead, next);
    }
}
