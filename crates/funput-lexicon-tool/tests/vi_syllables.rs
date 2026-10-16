//! The shipped Vietnamese syllable list, checked against the engine that will use it.
//!
//! Typo correction reads this list as a prior: a candidate in a higher band is more
//! likely to be what the user meant. A missing entry only costs a repair some
//! weight; a malformed one could let a real word be rewritten, and a band header
//! out of place would silently shift every entry after it. So every line is
//! validated here rather than trusted, and the lookup `funput-suggestions` builds
//! from the file is checked against the file itself.

use std::collections::BTreeSet;

use funput_core::is_complete_syllable;
use funput_suggestions::syllable_tier;
use unicode_normalization::UnicodeNormalization;

const LIST: &str = include_str!("../../funput-suggestions/data/syllables/vi.txt");

/// Every syllable with the band it is listed under, in file order.
fn entries() -> Vec<(&'static str, u8)> {
    let mut tier = 0;
    let mut entries = Vec::new();
    for line in LIST.lines().map(str::trim) {
        if let Some(band) = line.strip_prefix("## tier ") {
            tier = band.trim().parse().expect("a band header names its band");
        } else if !line.is_empty() && !line.starts_with('#') {
            assert_ne!(tier, 0, "{line:?} is listed before the first band header");
            entries.push((line, tier));
        }
    }
    entries
}

fn in_tier(tier: u8) -> Vec<&'static str> {
    entries()
        .into_iter()
        .filter(|(_, band)| *band == tier)
        .map(|(entry, _)| entry)
        .collect()
}

#[test]
fn the_bands_appear_once_each_and_in_order() {
    let mut bands = Vec::new();
    for (_, tier) in entries() {
        if bands.last() != Some(&tier) {
            bands.push(tier);
        }
    }
    assert_eq!(
        bands,
        [1, 2, 3, 4],
        "bands must run 1..4 with no gap or repeat"
    );
}

#[test]
fn every_entry_is_a_real_vietnamese_syllable() {
    let malformed: Vec<&str> = entries()
        .into_iter()
        .map(|(entry, _)| entry)
        .filter(|entry| !is_complete_syllable(entry))
        .collect();
    assert!(
        malformed.is_empty(),
        "these are not syllables the engine can compose: {malformed:?}"
    );
}

/// The lookup compares bytes against a lowercase NFC key, so an entry written any
/// other way could never be found.
#[test]
fn every_entry_is_written_in_lowercase_nfc() {
    let odd: Vec<&str> = entries()
        .into_iter()
        .map(|(entry, _)| entry)
        .filter(|entry| {
            let canonical: String = entry.nfc().flat_map(char::to_lowercase).collect();
            canonical != *entry
        })
        .collect();
    assert!(odd.is_empty(), "not lowercase NFC: {odd:?}");
}

#[test]
fn no_entry_is_listed_twice_in_any_band() {
    let mut seen = BTreeSet::new();
    let repeated: Vec<&str> = entries()
        .into_iter()
        .map(|(entry, _)| entry)
        .filter(|entry| !seen.insert(*entry))
        .collect();
    assert!(repeated.is_empty(), "listed more than once: {repeated:?}");
}

#[test]
fn the_list_is_worth_shipping() {
    // The floor is here so the list cannot quietly shrink to nothing; the ceiling
    // is here because the host learns every entry at startup, and that cost is
    // measured, not assumed. Each band must hold something, or the weights it
    // stands for have nothing to say.
    let count = entries().len();
    assert!(
        (1_200..=4_000).contains(&count),
        "{count} entries is outside the size this was measured for"
    );
    for tier in 1..=4 {
        assert!(!in_tier(tier).is_empty(), "band {tier} is empty");
    }
}

#[test]
fn the_words_that_hold_a_sentence_together_sit_in_band_one() {
    let first: BTreeSet<&str> = in_tier(1).into_iter().collect();
    for word in ["là", "của", "và", "có", "không", "được", "người", "một"] {
        assert!(first.contains(word), "{word} should be in band 1");
    }
}

/// The table `funput-suggestions` builds is what the keyboards read; it has to say
/// exactly what the file says, for every entry.
#[test]
fn the_lookup_agrees_with_the_file() {
    for (entry, tier) in entries() {
        assert_eq!(syllable_tier(entry), Some(tier), "{entry}");
    }
    assert_eq!(syllable_tier("dduwowfnh"), None);
}
