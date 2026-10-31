//! Predict with the shipped Vietnamese pairs behind it.
//!
//! These lean on three pairs of the shipped list (`cảm ơn`, `sức khoẻ`,
//! `tuy nhiên`). Dropping one from `data/pairs/vi.txt` means picking another here.

mod english;
mod prefix;

use funput_core::ToneStyle::{Modern, Traditional};

use crate::{Edit, EditKind, SeedLanguage, SuggestionConfig, SuggestionEngine};

fn engine(language: SeedLanguage) -> SuggestionEngine {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    engine.set_seed_language(language);
    engine
}

fn predicted(engine: &SuggestionEngine, previous: &str) -> Vec<String> {
    engine
        .suggest_with(Some(previous), "")
        .iter()
        .map(str::to_owned)
        .collect()
}

#[test]
fn a_fresh_store_predicts_from_the_shipped_pairs() {
    let engine = engine(SeedLanguage::Vietnamese(Modern));
    assert_eq!(predicted(&engine, "cảm"), ["ơn"]);
    assert_eq!(predicted(&engine, "Tuy"), ["nhiên"], "case does not matter");
    assert!(
        predicted(&engine, "của").is_empty(),
        "a flat context stays silent"
    );
}

#[test]
fn the_shipped_pairs_say_nothing_until_a_shell_turns_them_on() {
    let mut engine = engine(SeedLanguage::Off);
    assert!(predicted(&engine, "cảm").is_empty());
    engine.set_seed_language(SeedLanguage::Vietnamese(Modern));
    assert_eq!(predicted(&engine, "cảm"), ["ơn"]);
    engine.set_seed_language(SeedLanguage::Off);
    assert!(
        predicted(&engine, "cảm").is_empty(),
        "English typing hears nothing"
    );
}

#[test]
fn the_word_typed_after_the_context_learns_as_before() {
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    engine.learn_after(None, "cảm");
    assert_eq!(
        predicted(&engine, "cảm"),
        ["ơn"],
        "a recorded context still asks the list"
    );
    engine.learn_after(Some("cảm"), "ơn");
    engine.learn_after(Some("cảm"), "ơn");
    assert_eq!(
        predicted(&engine, "cảm"),
        ["ơn"],
        "now from the user's own pair"
    );
}

#[test]
fn the_users_own_pair_beats_the_list() {
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    engine.learn_after(None, "cảm");
    for _ in 0..2 {
        engine.learn_after(Some("cảm"), "thấy");
    }
    assert_eq!(predicted(&engine, "cảm"), ["thấy"]);
}

#[test]
fn the_list_steps_back_once_the_user_has_typed_the_context_enough() {
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    engine.learn_after(None, "cảm");
    for next in ["thấy", "giác"] {
        engine.learn_after(Some("cảm"), next);
    }
    assert_eq!(
        predicted(&engine, "cảm"),
        ["ơn"],
        "two sightings, no follower Predict trusts yet"
    );
    engine.learn_after(Some("cảm"), "xúc");
    assert!(
        predicted(&engine, "cảm").is_empty(),
        "three spread-out sightings are the user's answer, and it is silence"
    );
}

#[test]
fn a_blocked_word_is_never_offered() {
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    engine.apply_edit(Edit::new(EditKind::Block, "ơn"));
    assert!(predicted(&engine, "cảm").is_empty());
}

#[test]
fn a_word_blocked_in_one_tone_style_stays_blocked_in_the_other() {
    let mut engine = engine(SeedLanguage::Vietnamese(Traditional));
    engine.apply_edit(Edit::new(EditKind::Block, "khỏe"));
    assert!(predicted(&engine, "sức").is_empty());
    engine.set_seed_language(SeedLanguage::Vietnamese(Modern));
    assert!(
        predicted(&engine, "sức").is_empty(),
        "switching to the modern style must not bring back `khoẻ`"
    );
}

#[test]
fn clearing_the_users_data_keeps_the_shipped_pairs() {
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    engine.learn_after(None, "cảm");
    engine.reset().expect("an in-memory reset cannot fail");
    assert_eq!(predicted(&engine, "cảm"), ["ơn"]);
}

#[test]
fn the_bar_writes_the_tone_style_the_user_chose() {
    let modern = engine(SeedLanguage::Vietnamese(Modern));
    let traditional = engine(SeedLanguage::Vietnamese(Traditional));
    assert_eq!(predicted(&modern, "sức"), ["khoẻ"]);
    assert_eq!(predicted(&traditional, "sức"), ["khỏe"]);
}

#[test]
fn no_context_and_no_prefix_offers_nothing() {
    let engine = engine(SeedLanguage::Vietnamese(Modern));
    assert!(
        engine.suggest_with(None, "").is_empty(),
        "no context, nothing to follow"
    );
}
