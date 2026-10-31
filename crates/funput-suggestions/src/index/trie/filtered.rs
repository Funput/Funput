//! A lookup that follows every path a prefix could still become, by a per-letter rule.
//!
//! The exact trie spells each word with its marks, so `đường` sits under `đ`,`ư`,`ờ`.
//! A prefix typed before its tone (`đươ`) is no path there. This walk takes, at each
//! depth, every child whose letter the typed letter may stand for (`matches`), and
//! at the end merges the best three of every node it reached. Each matching word
//! passes through exactly one of those nodes, and a node's three are the best of
//! everything under it, so the merge is the true best three of all matches, words
//! that end right at the prefix included.
//!
//! Children whose own best word could not make the cut are skipped, and the walk stops
//! after [`MAX_VISITS`] nodes: bounded work on every keystroke, and no allocation.

use super::ArenaTrie;
use super::node::{Entry, NONE, TOP_K};
use super::ranking::ranks_before;
use crate::types::WordRecord;

/// How many nodes one lookup may visit.
const MAX_VISITS: usize = 64;

impl ArenaTrie {
    /// The best few live words whose first letters `matches` accepts against
    /// `typed`, letter by letter, `NONE` where there are fewer.
    pub(crate) fn find_matching(
        &self,
        typed: &str,
        words: &[WordRecord],
        matches: impl Fn(char, char) -> bool,
    ) -> [u32; TOP_K] {
        let mut best = [Entry::EMPTY; TOP_K];
        let length = typed.chars().count();
        if length == 0 {
            return [NONE; TOP_K];
        }
        // Each pending node with how many typed letters its path has consumed.
        let mut pending = [(0u32, 0usize); MAX_VISITS];
        let (mut queued, mut visited) = (1, 0);
        while visited < queued {
            let (node, depth) = pending[visited];
            visited += 1;
            if depth == length {
                for entry in self.nodes[node as usize].top {
                    offer(&mut best, entry, words);
                }
                continue;
            }
            let Some(letter) = typed.chars().nth(depth) else {
                continue;
            };
            let mut child = self.nodes[node as usize].first_child;
            while child != NONE && queued < MAX_VISITS {
                let below = &self.nodes[child as usize];
                if matches(letter, below.label) && could_improve(&best, below.top[0], words) {
                    pending[queued] = (child, depth + 1);
                    queued += 1;
                }
                child = below.next_sibling;
            }
        }
        best.map(|entry| entry.live_id(words))
    }
}

/// Whether a subtree whose best word is `top` could still place a word in `best`.
fn could_improve(best: &[Entry; TOP_K], top: Entry, words: &[WordRecord]) -> bool {
    top.word(words).is_some() && ranks_before(top, best[TOP_K - 1], words)
}

/// Puts `entry` into `best`, kept in rank order, unless it is dead, already there, or
/// ranks below all of it.
fn offer(best: &mut [Entry; TOP_K], entry: Entry, words: &[WordRecord]) {
    if entry.word(words).is_none()
        || best
            .iter()
            .any(|kept| kept.id == entry.id && kept.generation == entry.generation)
        || !ranks_before(entry, best[TOP_K - 1], words)
    {
        return;
    }
    best[TOP_K - 1] = entry;
    for at in (1..TOP_K).rev() {
        if ranks_before(best[at], best[at - 1], words) {
            best.swap(at, at - 1);
        }
    }
}
