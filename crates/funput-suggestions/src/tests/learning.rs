use proptest::prelude::*;

use super::*;

#[test]
fn second_use_promotes_and_ranking_is_deterministic() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    assert_eq!(engine.learn("không"), LearnOutcome::Recorded);
    assert!(engine.suggest("kh").is_empty());
    assert_eq!(engine.learn("không"), LearnOutcome::Promoted);
    learned(&mut engine, "khỏe", 3);
    learned(&mut engine, "khoa", 2);
    assert_eq!(texts(&engine, "kh"), ["khỏe", "khoa", "không"]);
}

#[test]
fn exact_and_folded_results_merge_without_duplicates() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    learned(&mut engine, "hoa", 4);
    learned(&mut engine, "hòa", 3);
    learned(&mut engine, "hóa", 2);
    assert_eq!(texts(&engine, "ho"), ["hoa", "hòa", "hóa"]);
    assert_eq!(texts(&engine, "hò"), ["hòa"]);
}

#[test]
fn decomposed_uppercase_query_matches_nfc_words() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    learned(&mut engine, "ánh", 2);
    assert_eq!(texts(&engine, "A\u{0301}"), ["ánh"]);
}

#[test]
fn capacity_evicts_one_off_words_and_remains_bounded() {
    let config = SuggestionConfig {
        max_words: 3,
        ..SuggestionConfig::default()
    };
    let mut engine = SuggestionEngine::in_memory(config);
    learned(&mut engine, "alpha", 2);
    engine.learn("beta");
    engine.learn("gamma");
    engine.learn("delta");
    assert_eq!(engine.stats().words, 3);
    assert_eq!(texts(&engine, "al"), ["alpha"]);
}

#[test]
fn invalid_or_oversized_tokens_are_ignored() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
        max_token_scalars: 4,
        ..SuggestionConfig::default()
    });
    assert_eq!(engine.learn(""), LearnOutcome::Ignored);
    assert_eq!(engine.learn("hai từ"), LearnOutcome::Ignored);
    assert_eq!(engine.learn("abcde"), LearnOutcome::Ignored);
    assert_eq!(engine.stats().words, 0);
}

proptest! {
    #[test]
    fn arbitrary_unicode_never_panics(value in any::<String>()) {
        let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
        let _ = engine.learn(&value);
        let _ = engine.suggest(&value);
    }
}
