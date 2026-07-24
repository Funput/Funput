//! The in-memory search index the engine looks words up in.
//!
//! - [`trie`] — the arena-backed prefix trie with a bounded top-3 per node
//!   ([`ArenaTrie`]).
//! - `ranking` — the ordering policy the trie keeps its top-3 by (`ranks_before`).
//! - [`normalize`] — folds raw input into the exact / accent-folded lookup keys.

pub(crate) mod normalize;
mod ranking;
mod trie;

pub(crate) use trie::{ArenaTrie, NONE, TOP_K};
