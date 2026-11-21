//! `vi.txt`: a first syllable and the syllables Predict offers after it.

use std::collections::BTreeSet;

use super::{Line, faults, fresh_engine, lines};

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

fn firsts() -> Vec<&'static str> {
    lines().into_iter().map(|(first, _)| first).collect()
}

#[test]
fn every_line_lists_one_to_three_followers() {
    let odd: Vec<Line> = lines()
        .into_iter()
        .filter(|(_, followers)| !(1..=MAX_FOLLOWERS).contains(&followers.len()))
        .collect();
    assert!(odd.is_empty(), "{odd:?}");
}

#[test]
fn every_syllable_is_written_the_way_the_lookup_reads() {
    let syllables = lines()
        .into_iter()
        .flat_map(|(first, followers)| std::iter::once(first).chain(followers));
    let wrong = faults(syllables);
    assert!(wrong.is_empty(), "{wrong:?}");
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
    let engine = fresh_engine();
    for (first, followers) in lines() {
        let offered: Vec<&str> = engine.suggest_with(Some(first), "").iter().collect();
        assert_eq!(offered, followers, "after {first}");
    }
}

/// Predict reads only the syllable before, so a first syllable that is also a
/// follower sits at the end of a word as much as at the start of one: `điện` in
/// `bưu điện`. The engine reads each line as the word it spells and says nothing
/// after it, which is what lets such a syllable start a line of its own.
#[test]
fn a_first_syllable_that_ends_a_listed_word_is_quiet_after_it() {
    let engine = fresh_engine();
    let firsts: BTreeSet<&str> = firsts().into_iter().collect();
    for (first, followers) in lines() {
        for last in followers.into_iter().filter(|last| firsts.contains(last)) {
            let offered: Vec<&str> = engine.suggest_in(&[first, last], "").iter().collect();
            assert!(offered.is_empty(), "after `{first} {last}`: {offered:?}");
        }
    }
}
