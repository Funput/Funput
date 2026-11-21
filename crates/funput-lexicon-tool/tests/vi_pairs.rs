//! The shipped Vietnamese pair list, checked against the engine that will use it.
//!
//! Predict offers the followers of a first syllable before the user has typed
//! anything, so a malformed line is not a missed suggestion but a wrong one, on every
//! install. Every line is validated here rather than trusted, and the lookup
//! `funput-suggestions` builds from the file is checked against the file itself.

use std::collections::BTreeSet;

use funput_core::{ToneStyle, is_complete_syllable, respell_tone};
use funput_suggestions::{SeedLanguage, SuggestionConfig, SuggestionEngine};
use unicode_normalization::UnicodeNormalization;

const LIST: &str = include_str!("../../funput-suggestions/data/pairs/vi.txt");

/// How many followers a line may list: the slots of the bar.
const MAX_FOLLOWERS: usize = 3;

/// Syllables anything can follow. No three followers account for most of what comes
/// after one of these, so a line starting with it would be a guess on the commonest
/// syllables there are. Listed here, not discovered, so a reviewer sees why a tempting
/// line is refused.
const FLAT: &[&str] = &[
    "của", "và", "là", "có", "không", "được", "cho", "với", "các", "những", "này", "đã", "khi",
    "thì", "mà", "nhưng", "vì", "nên", "thật", "rất", "một", "người", "đi",
];

/// A first syllable and the syllables listed after it, best first.
type Line = (&'static str, Vec<&'static str>);

/// Every line, in file order.
fn lines() -> Vec<Line> {
    LIST.lines()
        .map(str::trim)
        .filter(|line| !line.is_empty() && !line.starts_with('#'))
        .map(|line| {
            let mut syllables = line.split(' ');
            let first = syllables.next().expect("a line that is not empty");
            let followers: Vec<&str> = syllables.collect();
            assert!(
                (1..=MAX_FOLLOWERS).contains(&followers.len()),
                "{line:?} lists {} followers, not 1 to {MAX_FOLLOWERS}",
                followers.len()
            );
            (first, followers)
        })
        .collect()
}

fn firsts() -> Vec<&'static str> {
    lines().into_iter().map(|(first, _)| first).collect()
}

fn followers() -> BTreeSet<&'static str> {
    lines().into_iter().flat_map(|(_, next)| next).collect()
}

fn every_syllable() -> impl Iterator<Item = &'static str> {
    lines()
        .into_iter()
        .flat_map(|(first, followers)| std::iter::once(first).chain(followers))
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

/// The table derives the traditional spelling from the modern one and never the
/// other way round, so a traditional line would leave modern users with no key to
/// match and hand them a follower spelled the way they did not choose.
#[test]
fn every_side_is_written_in_the_modern_tone_style() {
    let traditional: Vec<&str> = every_syllable()
        .filter(|syllable| respell_tone(syllable, ToneStyle::Modern).is_some())
        .collect();
    assert!(
        traditional.is_empty(),
        "write these the modern way (`hoà`, not `hòa`): {traditional:?}"
    );
}

#[test]
fn no_first_syllable_is_listed_twice() {
    let mut seen = BTreeSet::new();
    let repeated: Vec<&str> = firsts()
        .into_iter()
        .filter(|first| !seen.insert(*first))
        .collect();
    assert!(
        repeated.is_empty(),
        "the table keeps one line a first syllable, so list its followers together: {repeated:?}"
    );
}

/// A follower listed twice would take two of the three slots to say one word, and
/// one that repeats the first syllable (`luôn luôn`) would be offered after itself.
#[test]
fn no_line_repeats_a_syllable() {
    let repeating: Vec<Line> = lines()
        .into_iter()
        .filter(|(first, followers)| {
            let distinct: BTreeSet<&str> = followers.iter().copied().collect();
            distinct.len() != followers.len() || distinct.contains(first)
        })
        .collect();
    assert!(repeating.is_empty(), "{repeating:?}");
}

#[test]
fn no_line_starts_with_a_syllable_anything_can_follow() {
    let flat: Vec<&str> = firsts()
        .into_iter()
        .filter(|first| FLAT.contains(first))
        .collect();
    assert!(flat.is_empty(), "these predict nothing well: {flat:?}");
}

/// First syllables that are also a follower on another line, kept on purpose. Each
/// one makes Predict fire after that other word too: `bưu điện` then a space offers
/// `thoại`. Listed so the cost is a decision, not an accident. (`dũng cảm` and
/// `nhiệm vụ` were dropped for this: `cảm ơn` and `trách nhiệm` are the words people
/// type.)
///
/// - `điện`: `bưu điện` is rare next to `điện thoại`.
/// - `xin`: `vắc xin` is rare next to `xin chào`.
const ALSO_FOLLOWER: &[&str] = &["điện", "xin"];

/// Every exception is a wrong guess accepted, so the list of them stays short. Past
/// this, the answer is in the engine (staying quiet after a listed word), not here.
const MAX_EXCEPTIONS: usize = 4;

/// Predict reads only the word before, so it fires after a syllable wherever it
/// sits: at the end of `trách nhiệm` as much as at the start of `nhiệm vụ`. A
/// first syllable that ends another listed word is a wrong guess built into the
/// list itself.
#[test]
fn no_first_syllable_ends_another_listed_word() {
    let followers = followers();
    let clashing: Vec<&str> = firsts()
        .into_iter()
        .filter(|first| followers.contains(first) && !ALSO_FOLLOWER.contains(first))
        .collect();
    assert!(
        clashing.is_empty(),
        "these also end another listed word, so Predict fires after it too: {clashing:?}"
    );
    assert!(ALSO_FOLLOWER.len() <= MAX_EXCEPTIONS);
}

/// An exception outlives its reason when its line changes: a stale entry would
/// quietly let the next clash on that syllable through.
#[test]
fn every_exception_is_still_a_clash() {
    let (firsts, followers) = (firsts(), followers());
    let stale: Vec<&str> = ALSO_FOLLOWER
        .iter()
        .copied()
        .filter(|syllable| !(firsts.contains(syllable) && followers.contains(syllable)))
        .collect();
    assert!(
        stale.is_empty(),
        "no longer both a first syllable and a follower, drop from ALSO_FOLLOWER: {stale:?}"
    );
}

#[test]
fn the_list_is_worth_shipping() {
    // The floor stops the list quietly shrinking to nothing; the ceiling is the
    // size the design budgeted for (`docs/features/vietnamese-seed-suggestion.md`).
    let count = lines().len();
    assert!(
        (50..=2_500).contains(&count),
        "{count} lines is outside the size this was designed for"
    );
}

/// What the keyboards read is the table `funput-suggestions` builds; on a store
/// that has learned nothing, it has to offer exactly what the file says.
#[test]
fn a_fresh_store_offers_exactly_what_the_file_says() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    engine.set_seed_language(SeedLanguage::Vietnamese(ToneStyle::Modern));
    for (first, followers) in lines() {
        let offered: Vec<&str> = engine.suggest_with(Some(first), "").iter().collect();
        assert_eq!(offered, followers, "after {first}");
    }
}
