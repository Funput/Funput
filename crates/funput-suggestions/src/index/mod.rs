//! The in-memory search index the engine looks words up in.
//!
//! - [`trie`] — the arena-backed prefix trie with a bounded top-3 per node
//!   ([`ArenaTrie`]), its node type and its ranking policy.
//! - [`normalize`] — folds raw input into the exact / accent-folded lookup keys.
//! - [`marks`] — whether a word is what a prefix could still become, by the marks
//!   typed so far; the one matching rule for the personal store and shipped data.

pub(crate) mod marks;
pub(crate) mod normalize;
mod trie;

pub(crate) use trie::{ArenaTrie, NONE, TOP_K};
