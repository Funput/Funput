//! The engine with a lexicon attached: personal words first, the lexicon only in
//! the slots they leave, and nothing about the personal store changed by it.

use std::io::Write;

use proptest::prelude::*;

use super::*;
use crate::lexicon::Lexicon;
use crate::{SuggestionConfig, SuggestionEngine};

/// The fixture written to disk and attached, so the tests go through the same
/// mapped path the keyboards will. The file must outlive the engine's use of it.
fn attach(engine: &mut SuggestionEngine, tsv: &str) -> tempfile::NamedTempFile {
    let mut file = tempfile::NamedTempFile::new().unwrap();
    file.write_all(&encode(tsv).unwrap()).unwrap();
    engine.attach_lexicon(file.path()).unwrap();
    file
}

fn with_fixture() -> (SuggestionEngine, tempfile::NamedTempFile) {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    let file = attach(&mut engine, &tsv(&fixture()));
    (engine, file)
}

fn texts(engine: &SuggestionEngine, previous: Option<&str>, prefix: &str) -> Vec<String> {
    engine
        .suggest_with(previous, prefix)
        .iter()
        .map(str::to_owned)
        .collect()
}

fn learned(engine: &mut SuggestionEngine, word: &str, uses: usize) {
    for _ in 0..uses {
        engine.learn(word);
    }
}

#[test]
fn an_empty_store_gets_the_lexicons_top_three() {
    let (engine, _file) = with_fixture();
    assert_eq!(texts(&engine, None, "wh"), ["what", "when", "which"]);
    assert_eq!(texts(&engine, None, "th"), ["the", "that", "they"]);
    assert_eq!(texts(&engine, None, "ip"), ["iPhone"]);
    assert!(engine.suggest("zz").is_empty());
}

#[test]
fn personal_words_come_first_in_their_own_order() {
    let (mut engine, _file) = with_fixture();
    learned(&mut engine, "thì", 3);
    learned(&mut engine, "thế", 2);
    assert_eq!(texts(&engine, None, "th"), ["thì", "thế", "the"]);

    // A context reorders the personal part; the lexicon still only fills.
    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "thế");
    assert_eq!(texts(&engine, Some("xin"), "th"), ["thế", "thì", "the"]);
}

#[test]
fn a_learned_word_is_not_offered_again_in_the_lexicons_case() {
    let (mut engine, _file) = with_fixture();
    learned(&mut engine, "iphone", 2);
    assert_eq!(texts(&engine, None, "ip"), ["iphone"]);
}

#[test]
fn a_full_personal_answer_and_a_prediction_are_left_alone() {
    let (mut engine, _file) = with_fixture();
    for (word, uses) in [("work", 4), ("world", 3), ("would", 2)] {
        learned(&mut engine, word, uses);
    }
    assert_eq!(texts(&engine, None, "wo"), ["work", "world", "would"]);

    engine.learn_after(None, "xin");
    engine.learn_after(Some("xin"), "chào");
    engine.learn_after(Some("xin"), "chào");
    assert_eq!(texts(&engine, Some("xin"), ""), ["chào"]);
    assert!(engine.suggest("thà").is_empty());
}

#[test]
fn a_failed_attach_keeps_the_lexicon_already_attached() {
    let (mut engine, _file) = with_fixture();
    assert!(engine.attach_lexicon("/nonexistent/en.lex").is_err());
    let mut damaged = tempfile::NamedTempFile::new().unwrap();
    damaged.write_all(b"FPLX not really").unwrap();
    assert!(engine.attach_lexicon(damaged.path()).is_err());
    assert_eq!(texts(&engine, None, "wh"), ["what", "when", "which"]);

    let _other = attach(&mut engine, "whale\t0\nwheat\t1\n");
    assert_eq!(texts(&engine, None, "wh"), ["whale", "wheat"]);
}

#[test]
fn reset_forgets_the_user_and_keeps_the_lexicon() {
    let (mut engine, _file) = with_fixture();
    learned(&mut engine, "thì", 2);
    engine.reset().unwrap();
    assert_eq!(texts(&engine, None, "th"), ["the", "that", "they"]);
}

/// Words that collide with the fixture, words that do not, and Vietnamese.
const VOCABULARY: &[&str] = &[
    "the", "then", "them", "that", "thì", "thế", "what", "when", "whale", "work", "word", "iphone",
    "an", "and", "anh", "ăn", "covid", "monday", "some",
];

proptest! {
    #![proptest_config(ProptestConfig::with_cases(64))]

    /// Whatever the user has typed, attaching the lexicon keeps the personal
    /// answer as its prefix and adds only lexicon words it does not already hold.
    #[test]
    fn the_personal_answer_is_always_the_head(
        learns in prop::collection::vec(prop::sample::select(VOCABULARY), 0..40),
        prefixes in prop::collection::vec("[a-z]{0,4}|th|wh|an|wo|ip", 1..12),
    ) {
        let mut plain = SuggestionEngine::in_memory(SuggestionConfig::default());
        let (mut merged, _file) = with_fixture();
        for word in &learns {
            plain.learn(word);
            merged.learn(word);
        }
        let lexicon = Lexicon::from_bytes(fixture_bytes()).unwrap();
        for prefix in &prefixes {
            let mine = texts(&plain, None, prefix);
            let both = texts(&merged, None, prefix);
            prop_assert_eq!(&both[..mine.len()], &mine[..]);
            let fills: Vec<String> = lexicon
                .top3(prefix)
                .iter()
                .filter(|word| !mine.iter().any(|m| m.eq_ignore_ascii_case(word)))
                .map(str::to_owned)
                .collect();
            let expected_len = (mine.len() + fills.len()).min(3);
            prop_assert_eq!(&both[mine.len()..], &fills[..expected_len - mine.len()]);
        }
    }
}
