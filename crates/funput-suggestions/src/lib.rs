//! Small, local-only personal suggestion engine.
//!
//! This crate is deliberately independent from the Vietnamese composition engine.
//! Platforms own it on a background worker; lookup never performs I/O.
//!
//! # Layout
//!
//! - `binary` — little-endian integers and the CRC-32 every on-disk format uses.
//! - `bigram/` — which words follow which, and the write path that learns them.
//! - `dictionary/` — the store as a list the user can read and edit word by word,
//!   what the bar never offers, and the edit inbox between the app and the keyboard.
//! - `engine/` — the [`SuggestionEngine`] facade, its [`SuggestionConfig`], and the
//!   learn / query / durability behaviours.
//! - `index/` — the in-memory search index (prefix trie, ranking, key normalization).
//! - `lexicon/` — the read-only English word list (`en.lex`) and its format.
//! - `persistence/` — the crash-safe on-disk snapshot + journal store.
//! - `syllables` — the shipped Vietnamese syllables, ranked in four bands, and the
//!   prior weight typo correction reads off them.
//! - `types` — the shared vocabulary (`WordRecord` and the public result types).

mod bigram;
mod binary;
mod dictionary;
mod engine;
mod index;
mod keep;
mod lexicon;
mod persistence;
mod syllables;
mod types;

pub use dictionary::{
    DictionaryEntry, DictionaryFilter, DictionaryQuery, DictionarySort, Edit, EditKind,
    EditOutcome, Origin, post_edits,
};
pub use engine::{SuggestionConfig, SuggestionEngine};
pub use syllables::{SYLLABLE_TIER_WEIGHTS, syllable_tier, syllable_weight};
pub use types::{LearnOutcome, SuggestionSet, SuggestionStats};

/// Building `en.lex` from `en.tsv`, for `funput-lexicon-tool`. Never enabled in
/// the libraries the keyboards link.
#[cfg(feature = "lexicon-build")]
pub mod lexicon_build {
    pub use crate::lexicon::encode::encode;
    pub use crate::lexicon::{Stats, verify};
}

#[cfg(test)]
mod tests;
