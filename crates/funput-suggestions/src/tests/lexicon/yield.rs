//! When the lexicon stays out: a Vietnamese store typing a marked word. And the
//! running count that decides it, checked against a recount after anything the
//! learn path, a flush, a reset or a reopen can do.

use std::io::Write;

use proptest::prelude::*;

use super::*;
use crate::engine::admission::promotion_threshold;
use crate::index::normalize;
use crate::{SuggestionConfig, SuggestionEngine};

fn engine(yield_after: u32, max_words: usize) -> SuggestionEngine {
    SuggestionEngine::in_memory(SuggestionConfig {
        lexicon_yield_after_words: yield_after,
        max_words,
        ..SuggestionConfig::default()
    })
}

fn attach(engine: &mut SuggestionEngine) -> tempfile::NamedTempFile {
    let mut file = tempfile::NamedTempFile::new().unwrap();
    file.write_all(&fixture_bytes()).unwrap();
    engine.attach_lexicon(file.path()).unwrap();
    file
}

/// Learns each word often enough to be offered, English ones included: a word the
/// language cannot spell waits for `unrecognized_promotion_uses` rather than two.
fn promoted(engine: &mut SuggestionEngine, words: &[&str]) {
    for word in words {
        for _ in 0..SuggestionConfig::default().unrecognized_promotion_uses {
            engine.learn(word);
        }
    }
}

fn sorted(engine: &SuggestionEngine, prefix: &str) -> Vec<String> {
    let mut words: Vec<String> = engine.suggest(prefix).iter().map(str::to_owned).collect();
    words.sort();
    words
}

/// What the running count must equal, found the slow way.
fn recount(engine: &SuggestionEngine) -> u32 {
    // Per word, not one threshold for all: a marked word the language cannot spell —
    // `đánb` — is promoted later than `đánh`, and the running count knows it.
    let marked = engine.words.iter().filter(|word| {
        word.uses >= promotion_threshold(&word.text, &engine.config)
            && normalize::is_marked(&word.text)
    });
    marked.count() as u32
}

#[test]
fn below_the_threshold_the_lexicon_still_fills_a_marked_answer() {
    let mut engine = engine(3, 5_000);
    let _file = attach(&mut engine);
    promoted(&mut engine, &["anh", "ăn"]);
    assert_eq!(engine.lexicon.vietnamese_words, 1);
    assert_eq!(sorted(&engine, "an"), ["and", "anh", "ăn"]);
}

#[test]
fn a_vietnamese_store_keeps_english_out_of_a_marked_answer_only() {
    let mut engine = engine(3, 5_000);
    let _file = attach(&mut engine);
    promoted(&mut engine, &["anh", "ăn", "thì", "đi", "hai", "work"]);
    assert_eq!(engine.lexicon.vietnamese_words, 3);
    assert_eq!(sorted(&engine, "an"), ["anh", "ăn"]);
    assert_eq!(sorted(&engine, "ha"), ["had", "hai", "have"]);
    assert_eq!(sorted(&engine, "wo"), ["word", "work"]);
    assert_eq!(sorted(&engine, "wh"), ["what", "when", "which"]);
}

#[test]
fn a_zero_threshold_yields_from_the_first_marked_word() {
    let mut engine = engine(0, 5_000);
    let _file = attach(&mut engine);
    assert_eq!(sorted(&engine, "an"), ["an", "and"]);
    promoted(&mut engine, &["ăn"]);
    assert_eq!(sorted(&engine, "an"), ["ăn"]);
}

#[test]
fn the_count_survives_a_reopen_that_shrinks_the_store() {
    let directory = tempfile::tempdir().unwrap();
    let config = SuggestionConfig::default();
    {
        let mut engine = SuggestionEngine::open(directory.path(), config).unwrap();
        promoted(&mut engine, &["ăn", "thì", "đi", "khỏe", "anh", "work"]);
        engine.learn("uống");
        engine.flush().unwrap();
        engine.compact().unwrap();
        promoted(&mut engine, &["được", "hai"]);
        engine.flush().unwrap();
    }
    let reopened = SuggestionEngine::open(directory.path(), config).unwrap();
    assert_eq!(reopened.lexicon.vietnamese_words, 5);
    assert_eq!(reopened.lexicon.vietnamese_words, recount(&reopened));

    let shrunk = SuggestionConfig {
        max_words: 4,
        ..config
    };
    let smaller = SuggestionEngine::open(directory.path(), shrunk).unwrap();
    assert_eq!(smaller.lexicon.vietnamese_words, recount(&smaller));
}

#[derive(Debug, Clone)]
enum Step {
    Learn(&'static str),
    Flush,
    Reset,
}

/// `đánb` earns its place here: marked like a Vietnamese word, spelled like nothing the
/// language has, so it is the one word whose promotion the two counts could disagree on.
const WORDS: &[&str] = &[
    "ăn", "thì", "đi", "khỏe", "được", "anh", "hai", "em", "con", "work", "the", "iphone", "đánb",
];

fn step() -> impl Strategy<Value = Step> {
    prop_oneof![
        12 => prop::sample::select(WORDS).prop_map(Step::Learn),
        2 => Just(Step::Flush),
        1 => Just(Step::Reset),
    ]
}

proptest! {
    #![proptest_config(ProptestConfig::with_cases(128))]

    /// A store small enough to evict on almost every new word, so the count
    /// goes up on promotion, down on eviction, and is recounted by rebuilds, all
    /// interleaved.
    #[test]
    fn the_running_count_always_matches_a_recount(
        steps in prop::collection::vec(step(), 1..300),
        max_words in 3usize..8,
    ) {
        let mut engine = engine(200, max_words);
        for step in steps {
            match step {
                Step::Learn(word) => {
                    engine.learn(word);
                }
                Step::Flush => engine.flush().unwrap(),
                Step::Reset => engine.reset().unwrap(),
            }
            prop_assert_eq!(engine.lexicon.vietnamese_words, recount(&engine));
        }
    }
}
