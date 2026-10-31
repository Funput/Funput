//! The shipped Vietnamese pair list, checked against the engine that will use it.
//!
//! Predict offers the second syllable of a pair before the user has typed anything,
//! so a malformed line is not a missed suggestion but a wrong one, on every install.
//! Every line is validated here rather than trusted, and the lookup
//! `funput-suggestions` builds from the file is checked against the file itself.

use std::collections::BTreeSet;

use funput_core::{ToneStyle, is_complete_syllable};
use funput_suggestions::{SeedLanguage, SuggestionConfig, SuggestionEngine};
use unicode_normalization::UnicodeNormalization;

const LIST: &str = include_str!("../../funput-suggestions/data/pairs/vi.txt");

/// Syllables anything can follow. Predict offers one word, so a pair starting with
/// one of these would be a guess on the commonest syllables there are. Listed here,
/// not discovered, so a reviewer sees why a tempting line is refused.
const FLAT: &[&str] = &[
    "của", "và", "là", "có", "không", "được", "cho", "với", "các", "những", "này", "đã", "khi",
    "thì", "mà", "nhưng", "vì", "nên", "xin", "thật", "rất", "một", "người", "đi",
];

/// Every pair, in file order.
fn pairs() -> Vec<(&'static str, &'static str)> {
    LIST.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(|line| {
            let (first, next) = line
                .split_once(' ')
                .unwrap_or_else(|| panic!("{line:?} is not two syllables"));
            assert!(!next.contains(' '), "{line:?} has more than two syllables");
            (first, next)
        })
        .collect()
}

fn every_syllable() -> impl Iterator<Item = &'static str> {
    pairs().into_iter().flat_map(|(first, next)| [first, next])
}

#[test]
fn every_side_is_a_real_vietnamese_syllable() {
    let malformed: Vec<&str> = every_syllable()
        .filter(|syllable| !is_complete_syllable(syllable))
        .collect();
    assert!(
        malformed.is_empty(),
        "these are not syllables the engine can compose: {malformed:?}"
    );
}

/// The lookup compares bytes against a lowercase NFC key, so an entry written any
/// other way could never be found.
#[test]
fn every_side_is_written_in_lowercase_nfc() {
    let odd: Vec<&str> = every_syllable()
        .filter(|syllable| {
            let canonical: String = syllable.nfc().flat_map(char::to_lowercase).collect();
            canonical != *syllable
        })
        .collect();
    assert!(odd.is_empty(), "not lowercase NFC: {odd:?}");
}

#[test]
fn no_first_syllable_is_listed_twice() {
    let mut seen = BTreeSet::new();
    let repeated: Vec<&str> = pairs()
        .into_iter()
        .map(|(first, _)| first)
        .filter(|first| !seen.insert(*first))
        .collect();
    assert!(
        repeated.is_empty(),
        "Predict offers one word, so a first syllable gets one line: {repeated:?}"
    );
}

#[test]
fn no_pair_starts_with_a_syllable_anything_can_follow() {
    let flat: Vec<&str> = pairs()
        .into_iter()
        .map(|(first, _)| first)
        .filter(|first| FLAT.contains(first))
        .collect();
    assert!(flat.is_empty(), "these predict nothing well: {flat:?}");
}

#[test]
fn the_list_is_worth_shipping() {
    // The floor stops the list quietly shrinking to nothing; the ceiling is the
    // size the design budgeted for (`docs/features/vietnamese-seed-suggestion.md`).
    let count = pairs().len();
    assert!(
        (50..=2_500).contains(&count),
        "{count} pairs is outside the size this was designed for"
    );
}

/// What the keyboards read is the table `funput-suggestions` builds; on a store
/// that has learned nothing, it has to offer exactly what the file says.
#[test]
fn a_fresh_store_offers_exactly_what_the_file_says() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    engine.set_seed_language(SeedLanguage::Vietnamese(ToneStyle::Modern));
    for (first, next) in pairs() {
        let offered: Vec<&str> = engine.suggest_with(Some(first), "").iter().collect();
        assert_eq!(offered, [next], "after {first}");
    }
}
