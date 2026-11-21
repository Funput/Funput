//! What the shipped pairs add to the bar after a space.
//!
//! They only fill the slots the user's own followers left empty, and they step
//! back where the user has shown a pair is not theirs: once a context has been
//! typed `context_evidence_seen` times, a shipped word stays only if it is one
//! of the words the user follows that context with.
//!
//! "One of the words" means one of the followers the store still holds, which is
//! `FOLLOWER_SLOTS` a context. A word that followed the context once and has
//! since lost its slot to commoner ones reads as never followed, so this is an
//! approximation, and it errs on the side of staying quiet.

use super::SeedSlot;
use super::pairs::Next;
use crate::bigram::follower::Followed;
use crate::engine::SuggestionEngine;
use crate::types::SuggestionSet;

impl SuggestionEngine {
    /// Fills the empty slots of `set` with the shipped words after `previous`,
    /// in the order the list ranks them. `followed` is what the store holds
    /// about `previous` as a context.
    ///
    /// Adds nothing while the data is off, for a word no pair starts with, and
    /// skips a word the user blocked in either tone style, one `set` already
    /// holds in either style, and one the user's own typing has stepped away from.
    pub(crate) fn fill_from_pairs<'a>(
        &'a self,
        previous: Option<&str>,
        followed: Followed<'_>,
        set: &mut SuggestionSet<'a>,
    ) {
        let SeedSlot::Vietnamese { style, pairs, .. } = self.seed else {
            return;
        };
        for next in previous.map_or(&[][..], |text| pairs.followers_after(text)) {
            let taken = next.spellings().any(|spelling| {
                self.blocked.contains(spelling) || set.iter().any(|kept| kept == spelling)
            });
            if !taken && self.still_follows(followed, next) {
                set.push(next.spelled(style));
            }
        }
    }

    /// Whether the user's own typing leaves room for `next` after the context.
    fn still_follows(&self, followed: Followed<'_>, next: &Next) -> bool {
        let Some((followers, seen)) = followed else {
            return true;
        };
        seen < self.config.context_evidence_seen
            || followers
                .iter()
                .filter_map(|slot| slot.word(&self.words))
                .any(|word| next.spellings().any(|spelling| spelling == word.text))
    }
}
