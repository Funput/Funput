//! Removing a word moves another into its slot. Nothing that referred to either
//! may end up pointing at the wrong word.

use std::collections::HashMap;

use proptest::prelude::*;

use super::{apply, engine};
use crate::tests::bigram::followers_of;
use crate::tests::{learned, texts};
use crate::{EditKind, SuggestionEngine};

fn pair(engine: &mut SuggestionEngine, first: &str, second: &str, times: usize) {
    for _ in 0..times {
        engine.learn_after(None, first);
        engine.learn_after(Some(first), second);
    }
}

#[test]
fn the_word_moved_into_the_hole_keeps_its_edges_both_ways() {
    let mut engine = engine();
    pair(&mut engine, "xin", "chào", 3);
    pair(&mut engine, "cảm", "ơn", 3);
    // "xin" sits in slot 0 and "ơn" is last, so forgetting "xin" moves "ơn".
    apply(&mut engine, EditKind::Forget, "xin");
    assert_eq!(followers_of(&engine, "cảm"), [("ơn".to_owned(), 3)]);
    assert_eq!(texts(&engine, "ơ"), ["ơn"]);
    assert!(texts(&engine, "xi").is_empty());
}

#[test]
fn edges_to_a_forgotten_word_die_with_it() {
    let mut engine = engine();
    pair(&mut engine, "xin", "chào", 3);
    pair(&mut engine, "xin", "lỗi", 2);
    apply(&mut engine, EditKind::Forget, "chào");
    assert_eq!(followers_of(&engine, "xin"), [("lỗi".to_owned(), 2)]);

    // A new word landing in the vacated slot inherits nothing.
    learned(&mut engine, "mới", 2);
    assert_eq!(followers_of(&engine, "xin"), [("lỗi".to_owned(), 2)]);
}

#[test]
fn forgetting_the_last_word_leaves_no_edge_behind() {
    let mut engine = engine();
    pair(&mut engine, "cảm", "ơn", 2);
    apply(&mut engine, EditKind::Forget, "ơn");
    assert!(followers_of(&engine, "cảm").is_empty());
    learned(&mut engine, "khác", 1);
    assert!(followers_of(&engine, "cảm").is_empty());
}

const WORDS: [&str; 6] = ["anh", "em", "chị", "bạn", "mình", "ta"];

proptest! {
    /// Learning and forgetting in any order agrees with a plain count per word,
    /// and every surviving edge points at a word that is really there.
    #[test]
    fn learning_and_forgetting_match_a_model(
        steps in prop::collection::vec((0usize..WORDS.len(), any::<bool>()), 1..80),
    ) {
        let mut engine = engine();
        let mut model: HashMap<&str, u32> = HashMap::new();
        let mut previous = None;
        for (index, forget) in steps {
            let word = WORDS[index];
            if forget {
                apply(&mut engine, EditKind::Forget, word);
                model.remove(word);
                previous = None;
            } else {
                engine.learn_after(previous, word);
                *model.entry(word).or_default() += 1;
                previous = Some(word);
            }
        }
        for word in WORDS {
            prop_assert_eq!(engine.frequency(word), model.get(word).copied().unwrap_or(0));
            for (next, _) in followers_of(&engine, word) {
                prop_assert!(model.contains_key(next.as_str()), "{} -> {}", word, next);
            }
        }
    }
}
