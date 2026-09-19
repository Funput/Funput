//! The two read-only questions typo correction asks: how often the user types a
//! word, and whether it is a word at all.

use std::io::Write;

use super::*;
use crate::lexicon::encode::encode;

fn engine() -> SuggestionEngine {
    SuggestionEngine::in_memory(SuggestionConfig::default())
}

#[test]
fn a_word_the_store_has_never_seen_has_no_frequency() {
    let engine = engine();
    assert_eq!(engine.frequency("đường"), 0);
    assert_eq!(engine.frequency(""), 0);
}

#[test]
fn frequency_counts_every_learn() {
    let mut engine = engine();
    learned(&mut engine, "đường", 5);
    learned(&mut engine, "đưởng", 1);
    assert_eq!(engine.frequency("đường"), 5);
    assert_eq!(engine.frequency("đưởng"), 1);
}

#[test]
fn frequency_matches_a_word_however_its_marks_are_encoded() {
    // Both sides normalize to NFC, so a candidate that arrives decomposed — which is
    // what an iOS text field can hand back — is the same word.
    let mut engine = engine();
    learned(&mut engine, "đường", 3);
    assert_eq!(engine.frequency("đường"), 3);
    assert_eq!(engine.frequency("\u{0111}u\u{031B}o\u{031B}\u{0300}ng"), 3);
    assert_eq!(
        engine.frequency("đừng"),
        0,
        "a different word, not a spelling"
    );
}

#[test]
fn a_word_longer_than_the_store_accepts_is_simply_unknown() {
    let engine = engine();
    assert_eq!(engine.frequency(&"a".repeat(1_000)), 0);
}

#[test]
fn a_learned_word_is_a_known_word() {
    let mut engine = engine();
    learned(&mut engine, "đường", 2);
    assert!(engine.is_known_word("đường"));
    assert!(!engine.is_known_word("đưườfng"));
}

#[test]
fn an_english_word_is_known_through_the_shipped_list() {
    // This is the veto that keeps typo correction off `text ` and `card `: the
    // engine can reach a Vietnamese syllable from them, and this is what says not to.
    let mut engine = engine();
    let mut file = tempfile::NamedTempFile::new().unwrap();
    file.write_all(&encode("text\t0\ncard\t1\nthe\t2\n").unwrap())
        .unwrap();
    engine.attach_lexicon(file.path()).unwrap();

    assert!(engine.is_known_word("text"));
    assert!(engine.is_known_word("card"));
    assert!(engine.is_known_word("TEXT"), "case is not the question");
    assert!(!engine.is_known_word("tex"), "a prefix is not a word");
    assert!(!engine.is_known_word("texts"));
    assert!(!engine.is_known_word("đường"));
    assert_eq!(
        engine.frequency("text"),
        0,
        "the list is not the user's store"
    );
}

#[test]
fn nothing_is_known_to_an_engine_with_no_lexicon_and_no_history() {
    let engine = engine();
    assert!(!engine.is_known_word("text"));
}
