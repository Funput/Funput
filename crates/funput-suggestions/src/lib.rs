//! Small, local-only personal suggestion engine.
//!
//! This crate is deliberately independent from the Vietnamese composition engine.
//! Platforms own it on a background worker; lookup never performs I/O.

mod config;
mod engine;
mod learning;
mod normalize;
mod persistence;
mod query;
mod ranking;
mod storage;
mod trie;
mod types;

pub use config::SuggestionConfig;
pub use engine::SuggestionEngine;
pub use types::{LearnOutcome, SuggestionSet, SuggestionStats};

#[cfg(test)]
mod tests;
