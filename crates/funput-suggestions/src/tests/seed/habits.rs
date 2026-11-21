//! The shipped pairs beside the user's own followers: who fills which slot once
//! the user has typed the context.

use funput_core::ToneStyle::{Modern, Traditional};

use super::{engine, predicted};
use crate::{SeedLanguage, SuggestionEngine};

fn followed(engine: &mut SuggestionEngine, context: &str, followers: &[(&str, usize)]) {
    engine.learn_after(None, context);
    for &(word, times) in followers {
        for _ in 0..times {
            engine.learn_after(Some(context), word);
        }
    }
}

#[test]
fn the_list_fills_the_slots_the_users_own_habit_left() {
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    followed(&mut engine, "cảm", &[("thấy", 3), ("ơn", 1)]);
    assert_eq!(
        predicted(&engine, "cảm"),
        ["thấy", "ơn"],
        "the user's habit first, then the shipped word they also type"
    );
}

#[test]
fn the_list_keeps_a_word_the_user_follows_the_context_with() {
    // Four different followers: no habit, and enough sightings for the user's
    // typing to outrank the list. `ơn` is one of them, so the list still stands.
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    followed(
        &mut engine,
        "cảm",
        &[("thấy", 1), ("giác", 1), ("xúc", 1), ("ơn", 1)],
    );
    assert_eq!(predicted(&engine, "cảm"), ["ơn"]);
}

#[test]
fn the_list_does_not_repeat_the_users_word_in_the_other_tone_style() {
    // The user writes `khỏe`; the list spells it `khoẻ`. One word, one slot,
    // whichever style the bar writes in.
    for style in [Modern, Traditional] {
        let mut engine = engine(SeedLanguage::Vietnamese(style));
        followed(&mut engine, "sức", &[("khỏe", 3)]);
        assert_eq!(predicted(&engine, "sức"), ["khỏe"], "{style:?}");
    }
}
