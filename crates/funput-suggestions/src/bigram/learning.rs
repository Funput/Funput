//! The write path: learn a token, and remember what it followed.

use super::follower::{FOLLOWER_SLOTS, Follower};
use super::slots;
use crate::engine::SuggestionEngine;
use crate::persistence::JournalEntry;
use crate::types::LearnOutcome;
use crate::types::WordRef;

impl SuggestionEngine {
    /// Learn `token`, and record that it followed `previous`.
    ///
    /// What counts as `previous` is the caller's business. This crate never looks
    /// at document context, so a platform that cannot vouch for what came before
    /// — a fresh focus, a moved caret, a sentence boundary — passes `None` and
    /// gets exactly the behaviour `learn` always had.
    ///
    /// The word before `previous` is not asked for: when `previous` itself was
    /// learned after a word, the journal already says so, and that is where the
    /// trigram comes from (see `ngram::chain`).
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
        let chained = context.is_some() && context == self.journal_chain.last();
        let older = chained.then(|| self.journal_chain.before_last()).flatten();

        let (outcome, word_id) = self.learn_inner(token);
        if outcome == LearnOutcome::Ignored {
            return outcome;
        }
        if record_pending {
            let token = self.words[word_id as usize].text.clone();
            self.push_pending(JournalEntry::Learned { token, chained });
        }
        let Some(learned) = WordRef::at(word_id, &self.words) else {
            return outcome;
        };
        if let Some(context) = context
            && context.is_live(&self.words)
        {
            self.record_edge(context.index as usize, word_id);
            if let Some(older) = older {
                self.record_trigram([older, context], learned);
            }
        }
        self.journal_chain.push(learned, chained);
        outcome
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

    /// Learning `next` may have evicted the older word, so it is checked here.
    fn record_trigram(&mut self, key: [WordRef; 2], next: WordRef) {
        if self.config.reads_trigrams() && key[0].is_live(&self.words) {
            self.trigrams.record(key, next, &self.words);
        }
    }
}
