use funput_core::ToneStyle::Modern;

use super::{engine, suggested, typed};
use crate::{SeedLanguage, SuggestionConfig, SuggestionEngine};

/// `ơn` on its own is led by `trời`, typed after `nhờ`; after `cảm` it is
/// followed by `bạn`, and after `biết` by three different words.
fn trained(config: SuggestionConfig) -> SuggestionEngine {
    let mut engine = SuggestionEngine::in_memory(config);
    typed(&mut engine, "nhờ ơn trời", 10);
    typed(&mut engine, "cảm ơn bạn", 3);
    typed(&mut engine, "biết ơn anh", 1);
    typed(&mut engine, "biết ơn chị", 1);
    typed(&mut engine, "biết ơn em", 1);
    engine
}

#[test]
fn two_words_of_context_speak_where_one_would_say_something_else() {
    let engine = trained(SuggestionConfig::default());
    assert_eq!(suggested(&engine, &["ơn"], ""), ["trời"]);
    assert_eq!(suggested(&engine, &["cảm", "ơn"], ""), ["bạn"]);
}

#[test]
fn an_unsure_trigram_stays_silent_instead_of_asking_the_word_before() {
    // After `biết ơn` the user has typed three different words: spread out, and
    // seen often enough to say so. `ơn` alone would have offered `trời`.
    let engine = trained(SuggestionConfig::default());
    assert!(suggested(&engine, &["biết", "ơn"], "").is_empty());
}

#[test]
fn a_trigram_with_no_evidence_steps_down_to_the_word_before() {
    let engine = trained(SuggestionConfig::default());
    assert_eq!(suggested(&engine, &["mới", "ơn"], ""), ["trời"]);
}

#[test]
fn order_two_reads_only_the_word_before() {
    let engine = trained(SuggestionConfig {
        context_order: 2,
        ..SuggestionConfig::default()
    });
    assert_eq!(suggested(&engine, &["cảm", "ơn"], ""), ["trời"]);
}

#[test]
fn only_the_last_two_words_of_a_longer_context_count() {
    let engine = trained(SuggestionConfig::default());
    assert_eq!(
        suggested(&engine, &["nhờ", "biết", "cảm", "ơn"], ""),
        ["bạn"]
    );
}

#[test]
fn rerank_puts_the_trigram_follower_before_the_word_befores() {
    let mut engine = engine();
    typed(&mut engine, "nhờ ơn bố", 5);
    typed(&mut engine, "cảm ơn bạn", 2);
    assert_eq!(suggested(&engine, &["ơn"], "b")[0], "bố");
    assert_eq!(suggested(&engine, &["cảm", "ơn"], "b")[..2], ["bạn", "bố"]);
}

#[test]
fn shipped_pairs_still_answer_when_no_personal_context_has_evidence() {
    let mut engine = engine();
    engine.set_seed_language(SeedLanguage::Vietnamese(Modern));
    assert_eq!(suggested(&engine, &["xin", "cảm"], ""), ["ơn"]);
}

#[test]
fn suggest_with_is_suggest_in_with_one_word() {
    let engine = trained(SuggestionConfig::default());
    let with: Vec<String> = engine
        .suggest_with(Some("ơn"), "")
        .iter()
        .map(str::to_owned)
        .collect();
    assert_eq!(with, suggested(&engine, &["ơn"], ""));
}
