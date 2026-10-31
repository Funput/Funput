use std::collections::HashSet;

use proptest::prelude::*;

use super::{all_trigrams, engine, trigram, typed};
use crate::EditKind;
use crate::tests::dictionary_apply;

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
