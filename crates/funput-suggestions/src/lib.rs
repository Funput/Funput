//! Small, local-only personal suggestion engine.
//!
//! This crate is deliberately independent from the Vietnamese composition engine.
//! Platforms own it on a background worker; lookup never performs I/O.
//!
//! # Layout
//!
//! - `engine/` — the [`SuggestionEngine`] facade, its [`SuggestionConfig`], and the
//!   learn / query / durability behaviours.
//! - `index/` — the in-memory search index (prefix trie, ranking, key normalization).
//! - `persistence/` — the crash-safe on-disk snapshot + journal store.
//! - `types` — the shared vocabulary (`WordRecord` and the public result types).

mod engine;
mod index;
mod persistence;
mod types;

pub use engine::{SuggestionConfig, SuggestionEngine};
pub use types::{LearnOutcome, SuggestionSet, SuggestionStats};

#[cfg(test)]
mod tests;
