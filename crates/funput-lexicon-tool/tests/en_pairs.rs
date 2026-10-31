//! The shipped English pair list, checked against the lexicon and the engine.
//!
//! The list shares the Vietnamese pairs' table and only speaks in Vietnamese mode, so
//! the checks that matter most are the ones keeping the two languages apart: no first
//! word a Vietnamese typist could produce, and no first word both lists claim.

use std::collections::BTreeSet;

use funput_core::{ToneStyle, is_complete_syllable};
use funput_suggestions::{SeedLanguage, SuggestionConfig, SuggestionEngine};

const LIST: &str = include_str!("../../funput-suggestions/data/pairs/en.txt");
const VIETNAMESE: &str = include_str!("../../funput-suggestions/data/pairs/vi.txt");
const LEXICON: &str = include_str!("../../funput-suggestions/data/lexicon/en.tsv");

/// Words anything can follow. Predict offers one word, so a pair led by one of these
/// would guess after the commonest words there are. Listed, not discovered, so a
/// reviewer sees why a tempting line (`as well`, `in order`) is refused.
const FLAT: &[&str] = &[
    "a", "an", "and", "as", "at", "be", "but", "by", "can", "do", "for", "from", "get", "go",
    "good", "have", "in", "is", "it", "just", "like", "make", "my", "not", "of", "on", "or", "so",
    "that", "the", "this", "to", "we", "what", "will", "with", "you",
];

fn pairs_of(list: &'static str) -> Vec<(&'static str, &'static str)> {
    list.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(|line| {
            let (first, next) = line
                .split_once(' ')
                .unwrap_or_else(|| panic!("{line:?} is not two words"));
            assert!(!next.contains(' '), "{line:?} has more than two words");
            (first, next)
        })
        .collect()
}

fn pairs() -> Vec<(&'static str, &'static str)> {
    pairs_of(LIST)
}

#[test]
fn every_word_is_lowercase_ascii_letters() {
    let odd: Vec<&str> = pairs()
        .into_iter()
        .flat_map(|(first, next)| [first, next])
        .filter(|word| word.is_empty() || !word.bytes().all(|byte| byte.is_ascii_lowercase()))
        .collect();
    assert!(odd.is_empty(), "not lowercase ASCII letters: {odd:?}");
}

/// The lexicon lists every English word Funput offers, after its blocklist; a pair word
/// it does not know is a typo, a name, or a word the blocklist keeps out.
#[test]
fn every_word_is_in_the_english_lexicon() {
    let known: BTreeSet<String> = LEXICON
        .lines()
        .filter(|line| !line.starts_with('#'))
        .filter_map(|line| line.split('\t').next())
        .map(str::to_owned)
        .collect();
    let unknown: Vec<&str> = pairs()
        .into_iter()
        .flat_map(|(first, next)| [first, next])
        .filter(|word| !known.contains(*word))
        .collect();
    assert!(
        unknown.is_empty(),
        "spelled differently, or not at all, in en.tsv: {unknown:?}"
    );
}

/// A first word Vietnamese typing can produce would hand a Vietnamese typist an
/// English prediction after a space.
#[test]
fn no_first_word_is_a_vietnamese_syllable() {
    let vietnamese: Vec<&str> = pairs()
        .into_iter()
        .map(|(first, _)| first)
        .filter(|first| is_complete_syllable(first))
        .collect();
    assert!(
        vietnamese.is_empty(),
        "these are also Vietnamese syllables: {vietnamese:?}"
    );
}

#[test]
fn no_pair_starts_with_a_word_anything_can_follow() {
    let flat: Vec<&str> = pairs()
        .into_iter()
        .map(|(first, _)| first)
        .filter(|first| FLAT.contains(first))
        .collect();
    assert!(flat.is_empty(), "these predict nothing well: {flat:?}");
}

#[test]
fn no_first_word_is_listed_twice_or_in_both_lists() {
    let mut seen: BTreeSet<&str> = pairs_of(VIETNAMESE)
        .into_iter()
        .map(|(first, _)| first)
        .collect();
    let repeated: Vec<&str> = pairs()
        .into_iter()
        .map(|(first, _)| first)
        .filter(|first| !seen.insert(*first))
        .collect();
    assert!(
        repeated.is_empty(),
        "one line per first word, across both lists: {repeated:?}"
    );
}

#[test]
fn the_list_stays_a_short_supplement() {
    // English is a courtesy here; the table's memory budget is set by the Vietnamese
    // list. The floor stops the list quietly emptying.
    let count = pairs().len();
    assert!(
        (20..=300).contains(&count),
        "{count} English pairs is outside the intended size"
    );
}

#[test]
fn a_fresh_store_in_vietnamese_mode_offers_exactly_what_the_file_says() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    engine.set_seed_language(SeedLanguage::Vietnamese(ToneStyle::Modern));
    for (first, next) in pairs() {
        let offered: Vec<&str> = engine.suggest_with(Some(first), "").iter().collect();
        assert_eq!(offered, [next], "after {first}");
    }
}
