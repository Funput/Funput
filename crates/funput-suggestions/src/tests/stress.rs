use tempfile::tempdir;

use super::*;

#[test]
fn stress_is_bounded_after_one_hundred_thousand_operations() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig::default());
    for index in 0..100_000 {
        engine.learn(&format!("word{:04}", index % 6_000));
        let _ = engine.suggest("word");
    }
    let stats = engine.stats();
    assert_eq!(stats.words, 5_000);
    assert!(stats.estimated_heap_bytes < 4 * 1024 * 1024, "{stats:?}");
}

#[test]
fn five_thousand_word_snapshot_stays_under_budget() {
    let directory = tempdir().unwrap();
    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    for index in 0..5_000 {
        let word = format!("word{index:04}");
        learned(&mut engine, &word, 2);
    }
    let stats = engine.stats();
    assert!(stats.estimated_heap_bytes < 4 * 1024 * 1024, "{stats:?}");
    assert!(engine.compact().unwrap() < 2 * 1024 * 1024);
}

#[test]
fn capacity_churn_does_not_grow_the_trie() {
    let mut engine = SuggestionEngine::in_memory(SuggestionConfig {
        max_words: 100,
        ..SuggestionConfig::default()
    });
    for index in 0..100 {
        learned(&mut engine, &format!("stable{index:03}"), 2);
    }
    let before = engine.stats();
    for index in 0..10_000 {
        engine.learn(&format!("transient{index:05}"));
    }
    let after = engine.stats();
    assert_eq!(after.words, 100);
    assert!(
        after.exact_nodes <= before.exact_nodes,
        "{before:?} -> {after:?}"
    );
}

#[test]
fn journal_auto_compacts_before_it_can_grow_unbounded() {
    let directory = tempdir().unwrap();
    let mut engine = SuggestionEngine::open(directory.path(), SuggestionConfig::default()).unwrap();
    let token = "abcdefghijklmnopqrstuvwxyzabcdef";
    for _ in 0..24 {
        for _ in 0..100 {
            engine.learn(token);
        }
        engine.flush().unwrap();
    }
    let stats = engine.stats();
    assert!(stats.journal_bytes < 64 * 1024, "{stats:?}");
    assert!(stats.last_snapshot_bytes > 0, "{stats:?}");
}
