//! The personal store matching a prefix by the marks typed so far, as the shipped
//! syllables do: Telex and VNI put the tone last, so `đươ` must reach a learned
//! `đường` before the tone key.

use crate::tests::{learned, texts};
use crate::{SuggestionConfig, SuggestionEngine};

fn engine() -> SuggestionEngine {
    SuggestionEngine::in_memory(SuggestionConfig::default())
}

#[test]
fn a_learned_word_is_found_before_its_tone_is_typed() {
    let mut engine = engine();
    learned(&mut engine, "đường", 2);
    assert_eq!(texts(&engine, "đươ"), ["đường"]);
    assert_eq!(texts(&engine, "đuơ"), ["đường"], "VNI `7` hooks only the `o` first");
    assert_eq!(texts(&engine, "duo"), ["đường"], "unmarked, as the folded trie always did");
    assert_eq!(texts(&engine, "đườ"), ["đường"], "and still by an exact prefix");
}

#[test]
fn a_mark_that_was_typed_still_has_to_be_right() {
    let mut engine = engine();
    learned(&mut engine, "thầy", 2);
    assert!(texts(&engine, "thà").is_empty(), "`à` is not `ầ`");
    assert_eq!(texts(&engine, "thâ"), ["thầy"]);
}

#[test]
fn exact_matches_still_come_first() {
    let mut engine = engine();
    learned(&mut engine, "dương", 5);
    learned(&mut engine, "đường", 2);
    // `dươ` is an exact prefix of `dương` and only a marks match of `đường`.
    assert_eq!(texts(&engine, "dươ"), ["dương", "đường"]);
}

#[test]
fn the_context_reorders_a_partly_marked_prefix_too() {
    let mut engine = engine();
    learned(&mut engine, "được", 5);
    engine.learn_after(None, "con");
    engine.learn_after(Some("con"), "đường");
    engine.learn_after(Some("con"), "đường");
    let words: Vec<&str> = engine.suggest_with(Some("con"), "đươ").iter().collect();
    assert_eq!(words.first(), Some(&"đường"), "{words:?}");
}
