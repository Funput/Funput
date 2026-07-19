use std::cmp::Ordering;

use crate::ranking::ranks_before;
use crate::types::WordRecord;

pub(crate) const NONE: u32 = u32::MAX;
pub(crate) const TOP_K: usize = 3;

#[derive(Debug, Clone)]
struct Node {
    label: char,
    first_child: u32,
    next_sibling: u32,
    top: [u32; TOP_K],
}

impl Node {
    fn new(label: char) -> Self {
        Self {
            label,
            first_child: NONE,
            next_sibling: NONE,
            top: [NONE; TOP_K],
        }
    }
}

#[derive(Debug, Clone)]
pub(crate) struct ArenaTrie {
    nodes: Vec<Node>,
}

impl ArenaTrie {
    pub(crate) fn new() -> Self {
        Self {
            nodes: vec![Node::new('\0')],
        }
    }

    pub(crate) fn clear(&mut self) {
        self.nodes.clear();
        self.nodes.push(Node::new('\0'));
    }

    pub(crate) fn node_count(&self) -> usize {
        self.nodes.len()
    }

    pub(crate) fn heap_bytes(&self) -> usize {
        self.nodes.capacity() * size_of::<Node>()
    }

    pub(crate) fn insert(
        &mut self,
        chars: impl Iterator<Item = char>,
        word_id: u32,
        words: &[WordRecord],
    ) {
        let mut node = 0u32;
        for ch in chars {
            node = self.ensure_child(node, ch);
            Self::update_top(&mut self.nodes[node as usize], word_id, words);
        }
    }

    pub(crate) fn find(&self, chars: impl Iterator<Item = char>) -> [u32; TOP_K] {
        let mut node = 0u32;
        let mut consumed = false;
        for ch in chars {
            consumed = true;
            let Some(next) = self.child(node, ch) else {
                return [NONE; TOP_K];
            };
            node = next;
        }
        if consumed {
            self.nodes[node as usize].top
        } else {
            [NONE; TOP_K]
        }
    }

    fn child(&self, parent: u32, label: char) -> Option<u32> {
        let mut current = self.nodes[parent as usize].first_child;
        while current != NONE {
            let node = &self.nodes[current as usize];
            match node.label.cmp(&label) {
                Ordering::Equal => return Some(current),
                Ordering::Greater => return None,
                Ordering::Less => current = node.next_sibling,
            }
        }
        None
    }

    fn ensure_child(&mut self, parent: u32, label: char) -> u32 {
        let mut previous = NONE;
        let mut current = self.nodes[parent as usize].first_child;
        while current != NONE {
            let node = &self.nodes[current as usize];
            match node.label.cmp(&label) {
                Ordering::Equal => return current,
                Ordering::Greater => break,
                Ordering::Less => {
                    previous = current;
                    current = node.next_sibling;
                }
            }
        }

        let index = self.nodes.len() as u32;
        let mut node = Node::new(label);
        node.next_sibling = current;
        self.nodes.push(node);
        if previous == NONE {
            self.nodes[parent as usize].first_child = index;
        } else {
            self.nodes[previous as usize].next_sibling = index;
        }
        index
    }

    fn update_top(node: &mut Node, word_id: u32, words: &[WordRecord]) {
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
}
