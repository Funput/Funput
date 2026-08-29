//! The order each node keeps its top-3 in, and the insertion that maintains it.

use super::node::{NONE, Node, TOP_K};
use crate::types::WordRecord;

fn ranks_before(left: u32, right: u32, words: &[WordRecord]) -> bool {
    if left == NONE {
        return false;
    }
    if right == NONE {
        return true;
    }
    let left = &words[left as usize];
    let right = &words[right as usize];
    left.uses
        .cmp(&right.uses)
        .then_with(|| left.last_used.cmp(&right.last_used))
        .then_with(|| right.text.cmp(&left.text))
        .is_gt()
}

pub(super) fn update_top(node: &mut Node, word_id: u32, words: &[WordRecord]) {
    let mut candidates = [NONE; TOP_K + 1];
    candidates[..TOP_K].copy_from_slice(&node.top);
    if !node.top.contains(&word_id) {
        candidates[TOP_K] = word_id;
    }

    for left in 0..candidates.len() {
        for right in (left + 1)..candidates.len() {
            if ranks_before(candidates[right], candidates[left], words) {
                candidates.swap(left, right);
            }
        }
    }
    node.top.copy_from_slice(&candidates[..TOP_K]);
}
