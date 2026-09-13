//! Small, local-only personal suggestion engine.
//!
//! This crate is deliberately independent from the Vietnamese composition engine.
//! Platforms own it on a background worker; lookup never performs I/O.
//!
//! # Layout
//!
//! - `binary` — little-endian integers and the CRC-32 every on-disk format uses.
//! - `bigram/` — which words follow which, and the write path that learns them.
//! - `engine/` — the [`SuggestionEngine`] facade, its [`SuggestionConfig`], and the
//!   learn / query / durability behaviours.
//! - `index/` — the in-memory search index (prefix trie, ranking, key normalization).
//! - `lexicon/` — the read-only English word list (`en.lex`) and its format.
//! - `persistence/` — the crash-safe on-disk snapshot + journal store.
//! - `types` — the shared vocabulary (`WordRecord` and the public result types).

mod bigram;
mod binary;
mod engine;
mod index;
#[cfg_attr(
    not(test),
    expect(dead_code, reason = "step 3 wires the lexicon into suggest_with")
)]
mod lexicon;
mod persistence;
mod types;

pub use engine::{SuggestionConfig, SuggestionEngine};
pub use types::{LearnOutcome, SuggestionSet, SuggestionStats};

/// Building `en.lex` from `en.tsv`, for `funput-lexicon-tool`. Never enabled in
/// the libraries the keyboards link.
#[cfg(feature = "lexicon-build")]
pub mod lexicon_build {
    pub use crate::lexicon::encode::encode;
}

#[cfg(test)]
mod tests;
