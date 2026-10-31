//! The short English pair list, which shares the Vietnamese table and only speaks in
//! Vietnamese mode. Leans on `thank you` being in `data/pairs/en.txt`.

use funput_core::ToneStyle::Modern;

use crate::{SeedLanguage, SuggestionConfig, SuggestionEngine};

fn engine(language: SeedLanguage) -> SuggestionEngine {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    engine.set_seed_language(language);
    engine
}

fn offered(engine: &SuggestionEngine, previous: &str, prefix: &str) -> Vec<String> {
    engine
        .suggest_with(Some(previous), prefix)
        .iter()
        .map(str::to_owned)
        .collect()
}

#[test]
fn english_typed_in_vietnamese_mode_gets_a_prediction() {
    let engine = engine(SeedLanguage::Vietnamese(Modern));
    assert_eq!(offered(&engine, "thank", ""), ["you"]);
    assert_eq!(
        offered(&engine, "Thank", ""),
        ["you"],
        "case does not matter"
    );
}

#[test]
fn nothing_shipped_speaks_while_the_data_is_off() {
    assert!(offered(&engine(SeedLanguage::Off), "thank", "").is_empty());
}

#[test]
fn the_users_own_english_pair_wins() {
    let mut engine = engine(SeedLanguage::Vietnamese(Modern));
    engine.learn_after(None, "thank");
    for _ in 0..2 {
        engine.learn_after(Some("thank"), "god");
    }
    assert_eq!(offered(&engine, "thank", ""), ["god"]);
}

#[test]
fn a_typed_prefix_puts_the_pair_first() {
    let engine = engine(SeedLanguage::Vietnamese(Modern));
    assert_eq!(
        offered(&engine, "thank", "yo").first().map(String::as_str),
        Some("you")
    );
}
