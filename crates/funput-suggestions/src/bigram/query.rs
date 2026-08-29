//! The read path: letting what came before reorder what comes next.
//!
//! A trie node remembers only its best few words by frequency, so a pair can be
//! sharp — "xin" is almost always followed by "chào" — while its target is rare
//! enough overall never to reach the reranker. The context is therefore allowed
//! to *add* a candidate, not only to move one. Everything it adds still matches
//! the prefix, so it is still a completion of what the user is typing.

use super::follower::{FOLLOWER_SLOTS, Follower};
use crate::engine::SuggestionEngine;
use crate::index::{NONE, TOP_K, normalize};
use crate::types::SuggestionSet;

impl SuggestionEngine {
    /// The words `prefix` could become, with the ones that have followed
    /// `previous` before moved to the front.
    ///
    /// `previous` is the caller's business, as it is for `learn_after`: a
    /// platform that cannot vouch for what came before passes `None` and gets
    /// exactly what `suggest` has always returned.
    pub fn suggest_with(&self, previous: Option<&str>, prefix: &str) -> SuggestionSet<'_> {
        let (ids, len) = self.prefix_candidates(prefix);
        let Some((context, _)) = previous.and_then(|text| self.context_slot(text)) else {
            return self.assemble(ids, len);
        };
        let (ids, len) = self.with_context(context, prefix, ids, len);
        self.assemble(ids, len)
    }

    fn with_context(
        &self,
        context: u32,
        prefix: &str,
        ids: [u32; TOP_K],
        len: usize,
    ) -> ([u32; TOP_K], usize) {
        let mut ranked = [NONE; FOLLOWER_SLOTS];
        let ranked_len = self.matching_followers(context, prefix, &mut ranked);
        if ranked_len == 0 {
            return (ids, len);
        }

        let mut merged = [NONE; TOP_K];
        let mut merged_len = 0;
        for id in ranked[..ranked_len].iter().chain(&ids[..len]) {
            if *id != NONE && !merged[..merged_len].contains(id) && merged_len < TOP_K {
                merged[merged_len] = *id;
                merged_len += 1;
            }
        }
        (merged, merged_len)
    }

    /// The live followers of `context` that `prefix` could still become, most
    /// used first. Four slots, so the ordering is done in place.
    fn matching_followers(
        &self,
        context: u32,
        prefix: &str,
        out: &mut [u32; FOLLOWER_SLOTS],
    ) -> usize {
        let Some(word) = self.words.get(context as usize) else {
            return 0;
        };
        let folded_allowed = normalize::folded_chars(prefix).eq(normalize::exact_chars(prefix));

        let mut slots = word.followers;
        let mut len = 0;
        for index in 0..FOLLOWER_SLOTS {
            let Some(strongest) = (index..FOLLOWER_SLOTS).max_by_key(|&at| slots[at].uses) else {
                break;
            };
            slots.swap(index, strongest);
            let follower = slots[index];
            if follower.uses < self.config.context_rerank_uses {
                // Sorted by uses, so nothing after this one clears the bar either.
                break;
            }
            if self.completes(follower, prefix, folded_allowed) {
                out[len] = follower.word;
                len += 1;
            }
        }
        len
    }

    fn completes(&self, follower: Follower, prefix: &str, folded_allowed: bool) -> bool {
        let Some(word) = follower.word(&self.words) else {
            return false;
        };
        if starts_with(word.text.chars(), normalize::exact_chars(prefix)) {
            return true;
        }
        folded_allowed
            && starts_with(
                normalize::folded_chars(&word.text),
                normalize::folded_chars(prefix),
            )
    }
}

fn starts_with(mut word: impl Iterator<Item = char>, prefix: impl Iterator<Item = char>) -> bool {
    prefix.into_iter().all(|scalar| word.next() == Some(scalar))
}
