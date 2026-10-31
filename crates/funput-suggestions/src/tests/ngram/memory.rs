//! The two promises: the table never grows past what it was given, and asking
//! for nothing costs nothing.

use super::{engine, typed};
use crate::ngram::Entry;
use crate::{SuggestionConfig, SuggestionEngine};

#[test]
fn nothing_is_allocated_until_a_trigram_is_learned() {
    let mut engine = engine();
    typed(&mut engine, "cảm ơn", 5);
    assert_eq!(engine.trigrams.heap_bytes(), 0);
    typed(&mut engine, "cảm ơn bạn", 1);
    assert_eq!(engine.trigrams.heap_bytes(), 4096 * size_of::<Entry<2>>());
}

#[test]
fn the_default_table_stays_within_its_budget() {
    assert!(size_of::<Entry<2>>() <= 52, "{}", size_of::<Entry<2>>());
    let engine = engine();
    assert!(engine.config.ngram_contexts * size_of::<Entry<2>>() <= 256 * 1024);
}

#[test]
fn a_long_session_never_grows_the_table() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
        ngram_contexts: 64,
        ..SuggestionConfig::default()
    });
    let words: Vec<String> = (0..300).map(|n| format!("t{n}")).collect();
    let mut previous: Option<&str> = None;
    let mut sizes = Vec::new();
    for step in 0..50_000usize {
        // A stride coprime with 300 walks every pair, so the table is under
        // constant pressure from contexts it has no room for.
        let word = &words[(step * 7 + step / 300) % words.len()];
        engine.learn_after(previous, word);
        previous = Some(word);
        if step % 5_000 == 4_999 {
            sizes.push(engine.trigrams.heap_bytes());
        }
    }
    assert!(
        sizes.iter().all(|&size| size == 64 * size_of::<Entry<2>>()),
        "{sizes:?}"
    );
    assert!(engine.trigrams.entries().count() <= 64);
}

#[test]
fn reset_hands_the_table_back() {
    let mut engine = engine();
    typed(&mut engine, "cảm ơn bạn", 1);
    assert!(engine.trigrams.heap_bytes() > 0);
    engine.reset().unwrap();
    assert_eq!(engine.trigrams.heap_bytes(), 0);
}

#[test]
fn the_configuration_is_clamped_to_what_the_table_can_be() {
    let engine = SuggestionEngine::in_memory(SuggestionConfig {
        context_order: 9,
        ngram_contexts: 1_000,
        ..SuggestionConfig::default()
    });
    assert_eq!(engine.config.context_order, 3);
    assert_eq!(engine.config.ngram_contexts, 1_024);

    let engine = SuggestionEngine::in_memory(SuggestionConfig {
        context_order: 0,
        ngram_contexts: 0,
        ..SuggestionConfig::default()
    });
    assert_eq!(engine.config.context_order, 2);
    assert_eq!(engine.config.ngram_contexts, crate::ngram::PROBE_WINDOW);
}
