//! Phần B: the shipped syllables and pairs filling what a prefix left empty.
//!
//! These lean on the shipped lists: `được` is in band 1 of `data/syllables/vi.txt`, and
//! `cảm ơn` is a shipped pair. Dropping either means picking another example here.

use std::io::Write;

use funput_core::ToneStyle::Modern;

use crate::lexicon::encode::encode;
use crate::seed::marks;
use crate::{Edit, EditKind, SeedLanguage, SuggestionConfig, SuggestionEngine};

fn engine(config: SuggestionConfig) -> SuggestionEngine {
    let mut engine = SuggestionEngine::in_memory(config);
    engine.set_seed_language(SeedLanguage::Vietnamese(Modern));
    engine
}

fn offered(engine: &SuggestionEngine, previous: Option<&str>, prefix: &str) -> Vec<String> {
    engine
        .suggest_with(previous, prefix)
        .iter()
        .map(str::to_owned)
        .collect()
}

fn attach_english(engine: &mut SuggestionEngine) -> tempfile::NamedTempFile {
    let mut file = tempfile::NamedTempFile::new().unwrap();
    file.write_all(&encode("the\t0\nthat\t1\nthey\t2\n").unwrap())
        .unwrap();
    engine.attach_lexicon(file.path()).unwrap();
    file
}

#[test]
fn a_fresh_store_completes_a_syllable_typed_without_its_tone() {
    let engine = engine(SuggestionConfig::default());
    let words = offered(&engine, None, "đươ");
    assert_eq!(words.first().map(String::as_str), Some("được"), "{words:?}");
    assert!(words.len() >= 2, "{words:?}");
    assert!(
        words.iter().all(|word| marks::completes("đươ", word)),
        "{words:?}"
    );
}

#[test]
fn the_syllable_typed_in_full_is_not_offered_back() {
    let engine = engine(SuggestionConfig::default());
    assert!(!offered(&engine, None, "ăn").contains(&"ăn".to_owned()));
}

#[test]
fn the_shipped_pair_comes_before_the_syllables() {
    let engine = engine(SuggestionConfig::default());
    assert_eq!(
        offered(&engine, Some("cảm"), "ơ")
            .first()
            .map(String::as_str),
        Some("ơn")
    );
}

#[test]
fn the_users_own_words_keep_their_slots() {
    // `đư`, not `đươ`: the store itself still matches marks exactly, so `đươ` would
    // not reach the learned `đường` (an open question in the design doc).
    let mut engine = engine(SuggestionConfig::default());
    engine.learn("đường");
    engine.learn("đường");
    let words = offered(&engine, None, "đư");
    assert_eq!(
        words.first().map(String::as_str),
        Some("đường"),
        "{words:?}"
    );
    assert_eq!(
        words.iter().filter(|word| *word == "đường").count(),
        1,
        "{words:?}"
    );
}

#[test]
fn a_blocked_word_is_never_offered() {
    let mut engine = engine(SuggestionConfig::default());
    engine.apply_edit(Edit::new(EditKind::Block, "được"));
    assert!(!offered(&engine, None, "đươ").contains(&"được".to_owned()));
}

#[test]
fn nothing_is_offered_while_the_data_is_off() {
    let mut engine = engine(SuggestionConfig::default());
    engine.set_seed_language(SeedLanguage::Off);
    assert!(offered(&engine, None, "đươ").is_empty());
}

#[test]
fn an_unmarked_prefix_leaves_the_last_slot_for_english() {
    let mut engine = engine(SuggestionConfig::default());
    let without = offered(&engine, None, "th");
    assert_eq!(
        without.len(),
        3,
        "the syllables fill every slot when English has nothing"
    );
    let _file = attach_english(&mut engine);
    let words = offered(&engine, None, "th");
    assert_eq!(words.len(), 3, "{words:?}");
    assert_eq!(words[..2], without[..2], "{words:?}");
    assert_eq!(words[2], "the", "{words:?}");
}

#[test]
fn a_marked_prefix_is_not_english() {
    let mut engine = engine(SuggestionConfig::default());
    let _file = attach_english(&mut engine);
    assert!(!offered(&engine, None, "thà").contains(&"the".to_owned()));
}

#[test]
fn shipped_syllables_do_not_make_english_give_way() {
    // A store that counts as Vietnamese from the first marked word: the lexicon gives
    // way to the user's marked words, but not to the shipped ones.
    let mut engine = engine(SuggestionConfig {
        lexicon_yield_after_words: 0,
        ..SuggestionConfig::default()
    });
    let _file = attach_english(&mut engine);
    assert!(offered(&engine, None, "th").contains(&"the".to_owned()));
}
