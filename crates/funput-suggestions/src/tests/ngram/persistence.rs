use std::collections::BTreeSet;

use proptest::prelude::*;
use tempfile::tempdir;

use super::{all_trigrams, suggested, trigram, typed};
use crate::{SuggestionConfig, SuggestionEngine};

fn open(directory: &std::path::Path, config: SuggestionConfig) -> SuggestionEngine {
    SuggestionEngine::open(directory, config).unwrap()
}

#[test]
fn trigrams_survive_a_compact() {
    let directory = tempdir().unwrap();
    {
        let mut engine = open(directory.path(), SuggestionConfig::default());
        typed(&mut engine, "cảm ơn bạn", 3);
        engine.compact().unwrap();
    }
    let reopened = open(directory.path(), SuggestionConfig::default());
    assert_eq!(trigram(&reopened, "cảm", "ơn"), [("bạn".to_owned(), 3)]);
    assert_eq!(suggested(&reopened, &["cảm", "ơn"], ""), ["bạn"]);
}

#[test]
fn trigrams_are_rebuilt_from_the_journal_alone() {
    let directory = tempdir().unwrap();
    {
        let mut engine = open(directory.path(), SuggestionConfig::default());
        typed(&mut engine, "cảm ơn bạn", 3);
        engine.flush().unwrap();
    }
    let reopened = open(directory.path(), SuggestionConfig::default());
    assert_eq!(trigram(&reopened, "cảm", "ơn"), [("bạn".to_owned(), 3)]);
}

#[test]
fn a_store_loaded_under_a_smaller_capacity_drops_its_trigrams_and_keeps_its_words() {
    let directory = tempdir().unwrap();
    {
        let mut engine = open(directory.path(), SuggestionConfig::default());
        typed(&mut engine, "cảm ơn bạn", 3);
        engine.learn("xa");
        engine.compact().unwrap();
    }
    let config = SuggestionConfig {
        max_words: 3,
        ..SuggestionConfig::default()
    };
    let reopened = open(directory.path(), config);
    assert!(all_trigrams(&reopened).is_empty());
    assert_eq!(reopened.trigrams.heap_bytes(), 0);
    assert_eq!(reopened.stats().words, 3);
}

const WORDS: [&str; 8] = ["anh", "em", "chị", "bạn", "mình", "ta", "xin", "chào"];

proptest! {
    /// With room to spare, whatever the order, breaks and word evictions, the table
    /// read back from the journal and then from a compacted snapshot is the table
    /// in memory.
    #[test]
    fn the_table_on_disk_is_the_table_in_memory(
        steps in prop::collection::vec((0usize..WORDS.len(), any::<bool>()), 1..120),
    ) {
        let directory = tempdir().unwrap();
        // Half the vocabulary fits, so learning keeps evicting context words.
        let config = SuggestionConfig { max_words: 4, ..SuggestionConfig::default() };
        let mut engine = open(directory.path(), config);
        let mut previous = None;
        for (index, chained) in steps {
            let word = WORDS[index];
            engine.learn_after(previous.filter(|_| chained), word);
            previous = Some(word);
        }
        let before = all_trigrams(&engine);
        engine.flush().unwrap();
        drop(engine);

        let mut replayed = open(directory.path(), config);
        prop_assert_eq!(all_trigrams(&replayed), before.clone());
        replayed.compact().unwrap();
        drop(replayed);

        let reopened = open(directory.path(), config);
        prop_assert_eq!(all_trigrams(&reopened), before);
    }

    /// A table with no room to spare: what survives a reload may differ, but
    /// nothing in it, before or after, is three words that were not typed in a row.
    #[test]
    fn a_full_table_only_ever_holds_typed_trigrams(
        steps in prop::collection::vec((0usize..WORDS.len(), any::<bool>()), 1..160),
    ) {
        let directory = tempdir().unwrap();
        let config = SuggestionConfig {
            max_words: 6,
            ngram_contexts: 8,
            ..SuggestionConfig::default()
        };
        let mut engine = open(directory.path(), config);
        let mut typed = BTreeSet::new();
        let mut run: Vec<&str> = Vec::new();
        for (index, chained) in steps {
            let word = WORDS[index];
            engine.learn_after(run.last().copied().filter(|_| chained), word);
            if !chained {
                run.clear();
            }
            run.push(word);
            if let [.., older, previous, next] = run[..] {
                typed.insert((older.to_owned(), previous.to_owned(), next.to_owned()));
            }
        }
        let held = |engine: &SuggestionEngine| -> Vec<(String, String, String)> {
            all_trigrams(engine)
                .into_iter()
                .flat_map(|(older, previous, _, followers)| {
                    followers
                        .into_iter()
                        .map(move |(next, _)| (older.clone(), previous.clone(), next))
                })
                .collect()
        };
        for trigram in held(&engine) {
            prop_assert!(typed.contains(&trigram), "{:?} was never typed", trigram);
        }
        engine.flush().unwrap();
        drop(engine);

        let mut replayed = open(directory.path(), config);
        for trigram in held(&replayed) {
            prop_assert!(typed.contains(&trigram), "replay invented {:?}", trigram);
        }
        replayed.compact().unwrap();
        drop(replayed);

        let reopened = open(directory.path(), config);
        for trigram in held(&reopened) {
            prop_assert!(typed.contains(&trigram), "the snapshot invented {:?}", trigram);
        }
    }
}
