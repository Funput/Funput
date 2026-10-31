//! What typing and editing do to the table: learning trigrams, and forgetting the
//! words they are made of.

use std::collections::HashSet;

use proptest::prelude::*;

use super::{all_trigrams, engine, trigram, typed};
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

#[test]
fn forgetting_a_context_word_drops_its_trigrams() {
    let mut engine = engine();
    typed(&mut engine, "cảm ơn bạn", 2);
    dictionary_apply(&mut engine, EditKind::Forget, "cảm");
    assert!(all_trigrams(&engine).is_empty());
}

#[test]
fn forgetting_a_follower_drops_it_and_its_sightings() {
    let mut engine = engine();
    typed(&mut engine, "cảm ơn bạn", 2);
    typed(&mut engine, "cảm ơn anh", 1);
    dictionary_apply(&mut engine, EditKind::Forget, "bạn");
    assert_eq!(trigram(&engine, "cảm", "ơn"), [("anh".to_owned(), 1)]);
    assert_eq!(
        all_trigrams(&engine)[0].2,
        1,
        "bạn's two sightings must go too"
    );
}

#[test]
fn a_word_moved_by_a_removal_keeps_its_trigrams() {
    // Slots: xa, bạn, cảm, ơn. `swap_remove` moves the last word, `ơn`, into the
    // slot `xa` leaves; the entry keyed on it hashes elsewhere now and has to be
    // found at its new slot and generation.
    let mut engine = engine();
    engine.learn("xa");
    engine.learn("bạn");
    typed(&mut engine, "cảm ơn bạn", 2);
    dictionary_apply(&mut engine, EditKind::Forget, "xa");
    assert_eq!(engine.words[0].text, "ơn");
    assert_eq!(trigram(&engine, "cảm", "ơn"), [("bạn".to_owned(), 2)]);
}

const WORDS: [&str; 6] = ["anh", "em", "chị", "bạn", "mình", "ta"];

proptest! {
    /// Learning and forgetting in any order leaves every entry pointing at words
    /// that are really there.
    #[test]
    fn every_entry_points_at_live_words(
        steps in prop::collection::vec((0usize..WORDS.len(), any::<bool>()), 1..120),
    ) {
        let mut engine = engine();
        let mut previous = None;
        let mut forgotten = HashSet::new();
        for (index, forget) in steps {
            let word = WORDS[index];
            if forget {
                dictionary_apply(&mut engine, EditKind::Forget, word);
                forgotten.insert(word);
                previous = None;
            } else {
                engine.learn_after(previous, word);
                forgotten.remove(word);
                previous = Some(word);
            }
        }
        for entry in engine.trigrams.entries() {
            prop_assert!(entry.is_live(&engine.words));
            for slot in entry.followers.iter().filter(|slot| !slot.is_free()) {
                prop_assert!(slot.word(&engine.words).is_some());
            }
        }
        for (older, previous, _, followers) in all_trigrams(&engine) {
            prop_assert!(!forgotten.contains(older.as_str()));
            prop_assert!(!forgotten.contains(previous.as_str()));
            for (next, _) in followers {
                prop_assert!(!forgotten.contains(next.as_str()));
            }
        }
    }
}
