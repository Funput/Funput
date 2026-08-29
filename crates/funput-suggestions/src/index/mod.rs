//! The in-memory search index the engine looks words up in.
//!
//! - [`trie`] — the arena-backed prefix trie with a bounded top-3 per node
//!   ([`ArenaTrie`]), its node type and its ranking policy.
//! - [`normalize`] — folds raw input into the exact / accent-folded lookup keys.

pub(crate) mod normalize;
mod trie;

pub(crate) use trie::{ArenaTrie, NONE, TOP_K};
