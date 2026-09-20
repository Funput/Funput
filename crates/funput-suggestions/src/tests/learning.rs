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
fn a_mistyped_word_takes_longer_to_be_offered_than_a_real_syllable() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    // `nb` is not a coda Vietnamese has, and no tone can make it one.
    assert_eq!(engine.learn("đánb"), LearnOutcome::Recorded);
    assert_eq!(engine.learn("đánb"), LearnOutcome::Updated);
    assert!(engine.suggest("đá").is_empty(), "two slips are still slips");
    assert_eq!(engine.learn("đánb"), LearnOutcome::Updated);
    assert_eq!(engine.learn("đánb"), LearnOutcome::Promoted);
    // Nothing is refused outright: a word typed this often is the user's word.
    assert_eq!(texts(&engine, "đá"), ["đánb"]);
}

#[test]
fn a_word_still_short_of_its_diacritics_waits_with_the_mistypes() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    // `chuc` is a syllable shape Vietnamese only finishes with a tone (`chúc`), so
    // until it has one it reads like something interrupted rather than a word.
    learned(&mut engine, "chuc", 2);
    assert!(engine.suggest("chu").is_empty());
    learned(&mut engine, "chúc", 2);
    assert_eq!(texts(&engine, "chu"), ["chúc"]);
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
        // Slot fillers again; see `eviction::bounded`.
        unrecognized_promotion_uses: 2,
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
