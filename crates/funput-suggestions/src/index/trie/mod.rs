//! The arena-backed prefix trie: every node on a word's path remembers the best
//! few words underneath it, so a lookup is a walk plus one array read.
//!
//! - `node` — the arena node and its sizing constants.
//! - `ranking` — the order a node keeps its top-3 in.

mod node;
mod ranking;

use std::cmp::Ordering;

use crate::types::WordRecord;

use node::Node;
pub(crate) use node::{NONE, TOP_K};
use ranking::update_top;

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
            update_top(&mut self.nodes[node as usize], word_id, words);
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
}
