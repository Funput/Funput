//! The write path: learn a token, and remember what it followed.

use super::follower::{FOLLOWER_SLOTS, Follower};
use super::slots;
use crate::engine::{MAX_TOKEN_SCALARS, SuggestionEngine};
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
        self.learn_after_inner(previous, token, true)
    }

    pub(crate) fn learn_after_inner(
        &mut self,
        previous: Option<&str>,
        token: &str,
        record_pending: bool,
    ) -> LearnOutcome {
        // Resolved before learning because learning `token` can evict `previous`,
        // and re-checked after because it may have just done so.
        let context = previous.and_then(|text| self.context_slot(text));

        // Replay reads pairs off adjacency, so a token may only be marked as
        // chained when its context really is what the journal wrote last.
        // Anything else — a sentence boundary, an ignored token in between, a
        // context evicted since — is left unchained, and replay then misses an
        // edge instead of inventing one.
        let chained = context.is_some() && context == self.journalled_previous;

        let (outcome, word_id) = self.learn_inner(token);
        if outcome == LearnOutcome::Ignored {
            return outcome;
        }
        if record_pending {
            let token = self.words[word_id as usize].text.clone();
            self.push_pending((token, chained));
        }
        if let Some((index, generation)) = context
            && self.words[index as usize].generation == generation
        {
            self.record_edge(index as usize, word_id);
        }
        self.journalled_previous = Some((word_id, self.words[word_id as usize].generation));
        outcome
    }

    /// The slot `previous` sits in, and the generation it sits there at.
    ///
    /// Normalizes onto the stack and then compares with `==`, which is the same
    /// shape `upsert_word` uses: the length check reads out of the record itself
    /// and rejects almost every word without touching its heap buffer. Comparing
    /// through `normalize::exact_chars` inside the loop instead would rebuild an
    /// NFC iterator per word and take a cache miss on each one — affordable on
    /// the learn path, and this now runs on every keystroke.
    pub(super) fn context_slot(&self, previous: &str) -> Option<(u32, u16)> {
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let mut len = 0;
        for scalar in normalize::exact_chars(previous) {
            if len + scalar.len_utf8() > buffer.len() {
                // Longer than anything `learn` would have accepted.
                return None;
            }
            len += scalar.encode_utf8(&mut buffer[len..]).len();
        }
        let key = std::str::from_utf8(&buffer[..len]).ok()?;
        self.words
            .iter()
            .position(|word| word.text == key)
            .map(|index| (index as u32, self.words[index].generation))
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
