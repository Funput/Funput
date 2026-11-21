//! The shipped lines after a word their first syllable ends.
//!
//! These lean on `dũng cảm` in `data/pairs/vi-ends.txt` and on the lines `cảm ơn`
//! and `bưu điện` / `điện thoại` in `vi.txt`. Dropping one means picking another here.

use funput_core::ToneStyle::Modern;

use super::engine;
use crate::{SeedLanguage, SuggestionEngine};

fn offered(engine: &SuggestionEngine, context: &[&str], prefix: &str) -> Vec<String> {
    let set = engine.suggest_in(context, prefix);
    set.iter().map(str::to_owned).collect()
}

#[test]
fn a_first_syllable_says_nothing_after_the_word_it_ends() {
    let engine = engine(SeedLanguage::Vietnamese(Modern));
    assert_eq!(offered(&engine, &["cảm"], ""), ["ơn"]);
    assert!(offered(&engine, &["dũng", "cảm"], "").is_empty());
    assert_eq!(
        offered(&engine, &["rất", "cảm"], ""),
        ["ơn"],
        "any other word before it leaves the line alone"
    );
}

#[test]
fn a_line_is_read_as_the_word_it_spells() {
    let engine = engine(SeedLanguage::Vietnamese(Modern));
    assert_eq!(offered(&engine, &["điện"], ""), ["thoại"]);
    assert!(offered(&engine, &["bưu", "điện"], "").is_empty());
}

#[test]
fn only_the_two_words_before_the_caret_are_read() {
    let engine = engine(SeedLanguage::Vietnamese(Modern));
    assert_eq!(offered(&engine, &["dũng", "rất", "cảm"], ""), ["ơn"]);
    assert!(offered(&engine, &["rất", "dũng", "cảm"], "").is_empty());
}

#[test]
fn the_prefix_path_stays_quiet_after_the_word_too() {
    let engine = engine(SeedLanguage::Vietnamese(Modern));
    // `ơn` is no shipped syllable on its own merits here: only the line offers it.
    assert!(offered(&engine, &["cảm"], "ơ").contains(&"ơn".to_owned()));
    let after_word = offered(&engine, &["dũng", "cảm"], "ơ");
    assert_eq!(
        after_word,
        offered(&engine, &[], "ơ"),
        "after the word, the prefix is completed as with no context at all"
    );
}

#[test]
fn the_users_own_habit_still_speaks_after_the_word() {
    // The list of word ends silences shipped guesses, never what the user types.
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    engine.learn_after(None, "dũng");
    engine.learn_after(Some("dũng"), "cảm");
    for _ in 0..3 {
        engine.learn_after(Some("cảm"), "lắm");
    }
    assert_eq!(offered(&engine, &["dũng", "cảm"], ""), ["lắm"]);
}

#[test]
fn nothing_is_silenced_while_the_shipped_data_is_off() {
    let mut engine = engine(SeedLanguage::Off);
    engine.learn_after(None, "cảm");
    for _ in 0..3 {
        engine.learn_after(Some("cảm"), "ơn");
    }
    assert_eq!(offered(&engine, &["dũng", "cảm"], ""), ["ơn"]);
}
