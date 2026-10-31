//! Letting what came before reorder what comes next.
//!
//! A trie node remembers only its best few words by frequency, so a pair can be
//! sharp — "xin" is almost always followed by "chào" — while its target is rare
//! enough overall never to reach the reranker. The context is therefore allowed
//! to *add* a candidate, not only to move one. Everything it adds still matches
//! the prefix, so it is still a completion of what the user is typing.

use super::Contexts;
use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::engine::{MAX_TOKEN_SCALARS, SuggestionEngine};
use crate::index::{NONE, TOP_K, marks, normalize};
use crate::ngram::MAX_CONTEXT;

impl SuggestionEngine {
    /// The prefix results, behind the followers of every context that the prefix
    /// could still become: the two words before first, then the word before.
    pub(super) fn with_context(
        &self,
        contexts: &Contexts<'_>,
        prefix: &str,
        ids: [u32; TOP_K],
        len: usize,
    ) -> ([u32; TOP_K], usize) {
        if contexts.iter().all(Option::is_none) {
            return (ids, len);
        }
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let Some(typed) = normalize::exact_into(prefix, &mut buffer) else {
            return (ids, len);
        };
        let mut ranked = [NONE; FOLLOWER_SLOTS * MAX_CONTEXT];
        let mut ranked_len = 0;
        for (followers, _) in contexts.iter().flatten() {
            ranked_len += self.matching_followers(followers, typed, &mut ranked[ranked_len..]);
        }
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

    /// The live followers that `typed` could still become, most used first.
    /// Four slots, so the ordering is done in place.
    fn matching_followers(
        &self,
        followers: &[Follower; FOLLOWER_SLOTS],
        typed: &str,
        out: &mut [u32],
    ) -> usize {
        let mut slots = *followers;
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
            if self.completes(follower, typed) {
                out[len] = follower.word;
                len += 1;
            }
        }
        len
    }

    /// Whether the follower is what `typed` could still become, by the marks typed.
    fn completes(&self, follower: Follower, typed: &str) -> bool {
        follower
            .word(&self.words)
            .is_some_and(|word| marks::completes(typed, &word.text))
    }
}
