//! The shipped Vietnamese pair lists, checked against the engine that will use them.
//!
//! Predict offers the followers of a first syllable before the user has typed
//! anything, so a malformed line is not a missed suggestion but a wrong one, on every
//! install. Every line is validated here rather than trusted, and the lookups
//! `funput-suggestions` builds from the files are checked against the files themselves.
//!
//! - `lines` — `vi.txt`: a first syllable and the syllables offered after it.
//! - `ends` — `vi-ends.txt`: the words a first syllable ends, after which nothing is.

mod ends;
mod lines;

use funput_core::{ToneStyle, is_complete_syllable, respell_tone};
use funput_suggestions::{SeedLanguage, SuggestionConfig, SuggestionEngine};
use unicode_normalization::UnicodeNormalization;

const LINES: &str = include_str!("../../../funput-suggestions/data/pairs/vi.txt");

/// A first syllable and the syllables listed after it, best first.
type Line = (&'static str, Vec<&'static str>);

/// Every row of `list` that is not blank or a comment, split on single spaces.
fn rows(list: &'static str) -> impl Iterator<Item = Vec<&'static str>> {
    list.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(|line| line.split(' ').collect())
}

/// Every line of `vi.txt`, in file order.
fn lines() -> Vec<Line> {
    rows(LINES).map(|row| (row[0], row[1..].to_vec())).collect()
}

/// Why `syllable` cannot ship as written, if it cannot.
///
/// The lookups compare bytes against a lowercase NFC key, so a syllable written any
/// other way could never be found. They derive the traditional spelling from the
/// modern one and never the other way round, so a traditional entry would leave modern
/// users with no key to match.
fn fault(syllable: &str) -> Option<&'static str> {
    let canonical: String = syllable.nfc().flat_map(char::to_lowercase).collect();
    if canonical != syllable {
        Some("not lowercase NFC")
    } else if !is_complete_syllable(syllable) {
        Some("not a syllable the engine can compose")
    } else if respell_tone(syllable, ToneStyle::Modern).is_some() {
        Some("not in the modern tone style (`hoà`, not `hòa`)")
    } else {
        None
    }
}

/// The faults of every syllable in `syllables`, for an assertion to print.
fn faults(syllables: impl Iterator<Item = &'static str>) -> Vec<(&'static str, &'static str)> {
    syllables
        .filter_map(|syllable| fault(syllable).map(|why| (syllable, why)))
        .collect()
}

/// A store that has learned nothing, with the shipped Vietnamese data on.
fn fresh_engine() -> SuggestionEngine {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    engine.set_seed_language(SeedLanguage::Vietnamese(ToneStyle::Modern));
    engine
}
