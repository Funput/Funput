//! Letting what came before reorder what comes next.
//!
//! A trie node remembers only its best few words by frequency, so a pair can be
//! sharp — "xin" is almost always followed by "chào" — while its target is rare
//! enough overall never to reach the reranker. The context is therefore allowed
//! to *add* a candidate, not only to move one. Everything it adds still matches
//! the prefix, so it is still a completion of what the user is typing.

use crate::bigram::follower::{FOLLOWER_SLOTS, Follower};
use crate::engine::MAX_TOKEN_SCALARS;
use crate::engine::SuggestionEngine;
use crate::index::{NONE, TOP_K, marks, normalize};

impl SuggestionEngine {
    pub(super) fn with_context(
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
        let mut buffer = [0u8; MAX_TOKEN_SCALARS * 4];
        let Some(typed) = normalize::exact_into(prefix, &mut buffer) else {
            return 0;
        };

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
