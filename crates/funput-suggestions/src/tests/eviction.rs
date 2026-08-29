//! What happens to the tries when a word slot is handed to a different word.

use crate::engine::REBUILD_AFTER_EVICTIONS;
use crate::tests::{learned, texts};
use crate::{SuggestionConfig, SuggestionEngine};

fn bounded(max_words: usize) -> SuggestionEngine {
    SuggestionEngine::in_memory(SuggestionConfig {
        max_words,
        ..SuggestionConfig::default()
    })
}

#[test]
fn an_evicted_word_is_not_resurrected_under_its_own_prefix() {
    let mut engine = bounded(2);
    learned(&mut engine, "alpha", 2);
    learned(&mut engine, "beta", 3);
    assert_eq!(texts(&engine, "al"), ["alpha"]);

    // "alpha" is the lowest ranked, so "gamma" takes its slot.
    learned(&mut engine, "gamma", 2);
    assert_eq!(engine.stats().words, 2);
    assert!(
        texts(&engine, "al").is_empty(),
        "the new tenant of alpha's slot must not answer under alpha's prefix"
    );
    assert_eq!(texts(&engine, "ga"), ["gamma"]);
    assert_eq!(texts(&engine, "be"), ["beta"]);
}

#[test]
fn learning_never_rebuilds_the_tries() {
    let mut engine = bounded(8);
    for index in 0..8 {
        learned(&mut engine, &format!("word{index}"), 2);
    }
    assert_eq!(engine.rebuilds, 0, "filling to capacity evicts nothing");

    for index in 8..8 + (REBUILD_AFTER_EVICTIONS as usize - 1) {
        learned(&mut engine, &format!("word{index}"), 2);
    }
    assert_eq!(
        engine.rebuilds, 0,
        "every eviction below the safety net must stay off the learn path"
    );
}

#[test]
fn flush_reclaims_what_learning_left_behind() {
    let mut engine = bounded(4);
    for index in 0..12 {
        learned(&mut engine, &format!("word{index:02}"), 2);
    }
    assert_eq!(engine.rebuilds, 0);
    let carrying_dead_entries = engine.exact.node_count();

    engine.flush().unwrap();
    assert_eq!(engine.rebuilds, 1);
    assert!(
        engine.exact.node_count() < carrying_dead_entries,
        "{carrying_dead_entries} nodes should shrink once the dead ones are swept"
    );
    assert_eq!(texts(&engine, "word11"), ["word11"]);
    assert!(texts(&engine, "word00").is_empty());

    engine.flush().unwrap();
    assert_eq!(engine.rebuilds, 1, "a clean engine must not rebuild again");
}

#[test]
fn the_safety_net_bounds_the_tries_without_a_flush() {
    let mut engine = bounded(4);
    for index in 0..200 {
        learned(&mut engine, &format!("word{index:03}"), 2);
    }
    assert!(
        engine.rebuilds >= 3,
        "196 evictions past a threshold of {REBUILD_AFTER_EVICTIONS} must sweep themselves"
    );

    // Left to itself the trie would carry all 200 words. What it may carry is the
    // four live ones plus at most one threshold's worth of dead ones.
    let while_churning = engine.exact.node_count();
    engine.flush().unwrap();
    let live_only = engine.exact.node_count();
    assert!(
        while_churning < live_only + 8 * REBUILD_AFTER_EVICTIONS as usize,
        "{while_churning} nodes while churning against {live_only} live"
    );
}
