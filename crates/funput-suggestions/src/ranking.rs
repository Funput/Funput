use crate::trie::NONE;
use crate::types::WordRecord;

pub(crate) fn ranks_before(left: u32, right: u32, words: &[WordRecord]) -> bool {
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
