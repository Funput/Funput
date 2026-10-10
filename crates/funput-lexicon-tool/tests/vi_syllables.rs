//! The shipped Vietnamese syllable list, checked against the engine that will use it.
//!
//! Typo correction asks this list whether a word is one anyone writes. A missing
//! entry only costs a repair; a malformed one could let a real word be rewritten, so
//! every line is validated here rather than trusted.

use std::collections::BTreeSet;

use funput_core::is_complete_syllable;

const LIST: &str = include_str!("../../funput-suggestions/data/syllables/vi.txt");

fn entries() -> Vec<&'static str> {
    LIST.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .collect()
}

#[test]
fn every_entry_is_a_real_vietnamese_syllable() {
    let malformed: Vec<&str> = entries()
        .into_iter()
        .filter(|entry| !is_complete_syllable(entry))
        .collect();
    assert!(
        malformed.is_empty(),
        "these are not syllables the engine can compose: {malformed:?}"
    );
}

#[test]
fn no_entry_is_listed_twice() {
    let mut seen = BTreeSet::new();
    let repeated: Vec<&str> = entries()
        .into_iter()
        .filter(|entry| !seen.insert(*entry))
        .collect();
    assert!(repeated.is_empty(), "listed more than once: {repeated:?}");
}

#[test]
fn the_list_is_worth_shipping() {
    // Small on purpose — see the file's own header. The floor is here so the list
    // cannot quietly shrink to nothing; the ceiling is here because the host learns
    // every entry at startup, and that cost is measured, not assumed.
    let count = entries().len();
    assert!(
        (400..=4_000).contains(&count),
        "{count} entries is outside the size this was measured for"
    );
}

#[test]
fn the_most_common_words_are_near_the_front() {
    // The order is read as a ranking. It only has to be roughly right, but the words
    // that hold a sentence together have to be in the first stretch of it.
    let entries = entries();
    let head: BTreeSet<&str> = entries.iter().take(60).copied().collect();
    for word in ["là", "của", "và", "có", "không", "được", "người", "một"] {
        assert!(head.contains(word), "{word} should be near the front");
    }
}
