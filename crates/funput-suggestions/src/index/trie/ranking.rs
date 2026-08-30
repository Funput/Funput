//! The order each node keeps its top-3 in, and the insertion that maintains it.

use super::node::{Entry, Node, TOP_K};
use crate::types::WordRecord;

/// Dead entries rank last, so a live word always displaces one.
fn ranks_before(left: Entry, right: Entry, words: &[WordRecord]) -> bool {
    let Some(left) = left.word(words) else {
        return false;
    };
    let Some(right) = right.word(words) else {
        return true;
    };
    left.uses
        .cmp(&right.uses)
        .then_with(|| left.last_used.cmp(&right.last_used))
        .then_with(|| right.text.cmp(&left.text))
        .is_gt()
}

pub(super) fn update_top(node: &mut Node, entry: Entry, words: &[WordRecord]) {
    let mut candidates = [Entry::EMPTY; TOP_K + 1];
    candidates[..TOP_K].copy_from_slice(&node.top);
    // An id already present but at another generation is a dead entry for some
    // other word, not this one: let both compete and the dead one sort out.
    let present = node
        .top
        .iter()
        .any(|slot| slot.id == entry.id && slot.generation == entry.generation);
    if !present {
        candidates[TOP_K] = entry;
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
