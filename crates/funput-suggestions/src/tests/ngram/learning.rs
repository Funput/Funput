use super::{engine, trigram, typed};
use crate::tests::dictionary_apply;
use crate::{EditKind, SuggestionConfig, SuggestionEngine};

#[test]
fn typing_three_words_in_a_row_counts_the_third_after_the_first_two() {
    let mut engine = engine();
    typed(&mut engine, "cảm ơn bạn", 2);
    typed(&mut engine, "cảm ơn anh", 1);
    assert_eq!(
        trigram(&engine, "cảm", "ơn"),
        [("bạn".to_owned(), 2), ("anh".to_owned(), 1)]
    );
}

#[test]
fn a_boundary_between_the_first_two_words_learns_no_trigram() {
    let mut engine = engine();
    engine.learn_after(None, "cảm");
    engine.learn_after(None, "ơn");
    engine.learn_after(Some("ơn"), "bạn");
    assert!(trigram(&engine, "cảm", "ơn").is_empty());
}

#[test]
fn the_learned_trigram_comes_from_what_the_journal_chained_not_a_skipped_word() {
    // `xyz…` is too long to learn, so it is ignored and breaks the chain: `bạn`
    // did not follow `ơn` as far as the journal knows.
    let mut engine = engine();
    engine.learn_after(None, "cảm");
    engine.learn_after(Some("cảm"), "ơn");
    let long = "x".repeat(64);
    engine.learn_after(Some("ơn"), &long);
    engine.learn_after(Some(&long), "bạn");
    assert!(trigram(&engine, "cảm", "ơn").is_empty());
}

#[test]
fn order_two_learns_no_trigram_and_allocates_no_table() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
        context_order: 2,
        ..SuggestionConfig::default()
    });
    typed(&mut engine, "cảm ơn bạn", 3);
    assert!(trigram(&engine, "cảm", "ơn").is_empty());
    assert_eq!(engine.trigrams.heap_bytes(), 0);
}

#[test]
fn a_blocked_word_never_enters_a_trigram() {
    let mut engine = engine();
    dictionary_apply(&mut engine, EditKind::Block, "bạn");
    typed(&mut engine, "cảm ơn bạn", 3);
    assert!(trigram(&engine, "cảm", "ơn").is_empty());
}

#[test]
fn a_context_the_journal_did_not_just_write_learns_no_trigram() {
    // The journal ends `cảm ơn`, but the caller vouches for `bạn` before `anh`.
    // Replay could never rebuild `[cảm, bạn] → anh`, so it is not learned.
    let mut engine = engine();
    engine.learn("bạn");
    typed(&mut engine, "cảm ơn", 1);
    engine.learn_after(Some("bạn"), "anh");
    assert!(trigram(&engine, "cảm", "bạn").is_empty());
    assert!(trigram(&engine, "ơn", "bạn").is_empty());
}
