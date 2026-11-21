//! `vi-ends.txt`: the words a first syllable ends, after which Predict says nothing.
//!
//! A word here that the engine cannot find silences nothing, and one whose last
//! syllable starts no line silences nothing worth silencing. Both would sit in the
//! file looking like protection.

use std::collections::BTreeSet;

use super::{faults, fresh_engine, lines, rows};

const ENDS: &str = include_str!("../../../funput-suggestions/data/pairs/vi-ends.txt");

/// Every listed word, as its two syllables.
fn words() -> Vec<(&'static str, &'static str)> {
    rows(ENDS)
        .map(|row| match row[..] {
            [first, last] => (first, last),
            _ => panic!("{row:?} is not two syllables"),
        })
        .collect()
}

#[test]
fn every_syllable_is_written_the_way_the_lookup_reads() {
    let wrong = faults(words().into_iter().flat_map(|(first, last)| [first, last]));
    assert!(wrong.is_empty(), "{wrong:?}");
}

#[test]
fn every_word_ends_where_a_line_starts() {
    let firsts: BTreeSet<&str> = lines().into_iter().map(|(first, _)| first).collect();
    let idle: Vec<_> = words()
        .into_iter()
        .filter(|(_, last)| !firsts.contains(last))
        .collect();
    assert!(
        idle.is_empty(),
        "no line of vi.txt starts with the last syllable of: {idle:?}"
    );
}

/// The engine reads the lines of `vi.txt` as words too, so repeating one here says
/// nothing new, and hides that the line is what does the work.
#[test]
fn no_word_is_listed_twice_or_spelled_by_a_line() {
    let spelled: BTreeSet<(&str, &str)> = lines()
        .into_iter()
        .flat_map(|(first, followers)| followers.into_iter().map(move |last| (first, last)))
        .collect();
    let mut seen = BTreeSet::new();
    let redundant: Vec<_> = words()
        .into_iter()
        .filter(|word| spelled.contains(word) || !seen.insert(*word))
        .collect();
    assert!(redundant.is_empty(), "{redundant:?}");
}

#[test]
fn the_lines_are_quiet_after_every_word_and_speak_after_its_last_syllable_alone() {
    let engine = fresh_engine();
    for (first, last) in words() {
        let after_word: Vec<&str> = engine.suggest_in(&[first, last], "").iter().collect();
        assert!(
            after_word.is_empty(),
            "after `{first} {last}`: {after_word:?}"
        );
        assert!(
            !engine.suggest_with(Some(last), "").is_empty(),
            "`{last}` on its own still starts a line"
        );
    }
}
