//! The arena node the prefix trie is built from, and the two constants that size
//! it: the sentinel for "no node" and how many suggestions a node remembers.

pub(crate) const NONE: u32 = u32::MAX;
pub(crate) const TOP_K: usize = 3;

#[derive(Debug, Clone)]
pub(super) struct Node {
    pub(super) label: char,
    pub(super) first_child: u32,
    pub(super) next_sibling: u32,
    pub(super) top: [u32; TOP_K],
}

impl Node {
    pub(super) fn new(label: char) -> Self {
        Self {
            label,
            first_child: NONE,
            next_sibling: NONE,
            top: [NONE; TOP_K],
        }
    }
}
